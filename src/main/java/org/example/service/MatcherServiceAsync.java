package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.kafka.KafkaProducerService;
import org.example.redis.RedisService;
import org.example.service.match.MatchMeta;
import org.example.service.match.MatchResult;
import org.example.untils.CachedTemplate;
import org.example.untils.Message;
import org.example.untils.TextSimilarityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 * MatcherServiceAsync — асинхронний сервіс для семантичного порівняння документів з шаблонами.
 * Основні задачі:
 * - Завантаження шаблонів із кешу
 * - Обчислення embedding-векторів для кожного рядка документа
 * - Пошук найкращого шаблону за cosine similarity
 * - Збереження результатів у Redis
 * - Надсилання повідомлення клієнту через Kafka
 */
@Service
public class MatcherServiceAsync {

    /**
     * Логер для запису повідомлень про помилки та статусу виконання
     */
    private static final Logger logger = LoggerFactory.getLogger(MatcherServiceAsync.class);

    /**
     * Поріг схожості cosine similarity, при якому фрагмент вважається релевантним
     */
    private static final double SIMILARITY_THRESHOLD = 0.75;

    /**
     * Сервіс для збереження та отримання даних з Redis
     */
    private final RedisService redisService;

    /**
     * Сервіс для кешування шаблонів (включає embedding та фрагменти)
     */
    private final TemplateCacheService templateCacheService;

    /**
     * Jackson-маршалізатор для роботи з JSON-об'єктами
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Kafka-продюсер для надсилання результатів клієнту
     */
    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Зберігає останній відсоток прогресу, щоб уникнути дублювання повідомлень
     */
    private int lastSentPercent = -1;

    /**
     * Конструктор класу, ініціалізує сервіси кешу шаблонів і Redis
     */
    public MatcherServiceAsync(TemplateCacheService templateCacheService, RedisService redisService) {
        this.templateCacheService = templateCacheService;
        this.redisService = redisService;
    }

    /**
     * Основний метод для пошуку найкращого шаблону до переданого документа.
     *
     * @param sender ідентифікатор відправника (напр. client1 або insider)
     * @param doc назва або ідентифікатор документа
     * @param lines список рядків тексту документа
     */
    public void matchDocument(String sender, String doc, List<String> lines) {
        try {
            /**
             * Поточний найвищий бал подібності серед усіх шаблонів
             */
            double highestScore = -1;

            /**
             * Назва шаблону з найвищим балом відповідності
             */
            String bestTemplateName = null;

            /**
             * Карта знайдених відповідностей: ключ — назва поля, значення — рядок із документа
             */
            Map<String, String> bestResult = null;

            /**
             * JSON-модель шаблону з усіма фрагментами (ключ — поле, значення — список прикладів)
             */
            Map<String, List<String>> bestJsonModel = null;

            /**
             * Всі знайдені MatchResult для кожного шаблону
             */
            Map<String, List<MatchResult>> bestJsonMatchResult = new HashMap<>();

            /**
             * Статистика по кожному шаблону (назва, загальний бал, кількість збігів)
             */
            List<MatchMeta> matchStats = new ArrayList<>();

            /**
             * Усі доступні шаблони, завантажені з кешу
             */
            Map<String, CachedTemplate> allTemplates = templateCacheService.getTemplates();

            /**
             * Загальна кількість шаблонів для аналізу
             */
            int totalTemplates = allTemplates.size();

            /**
             * Лічильник оброблених шаблонів (для оновлення прогресу)
             */
            int processedTemplates = 0;

            for (Map.Entry<String, CachedTemplate> entry : allTemplates.entrySet()) {
                String fileName = entry.getKey();
                CachedTemplate cachedTemplate = entry.getValue();

                Map<String, List<String>> templateFragments = cachedTemplate.getFragments();
                Map<String, List<float[]>> templateEmbeddings = cachedTemplate.getEmbeddings();

                Map<String, String> result = new LinkedHashMap<>();
                double totalScore = 0.0;
                List<MatchResult> currentMatchResults = new ArrayList<>();

                for (String line : lines) {
                    String cleaned = line.replaceAll("\\s+", " ").trim();
                    if (cleaned.isBlank()) continue;

                    float[] lineEmb = templateCacheService.getPredictor().predict(cleaned);

                    String bestKey = null;
                    String bestFragment = null;
                    double bestScore = -1;

                    for (var e : templateEmbeddings.entrySet()) {
                        String key = e.getKey();
                        List<float[]> embeddings = e.getValue();
                        List<String> fragments = templateFragments.get(key);

                        for (int i = 0; i < embeddings.size(); i++) {
                            double score = TextSimilarityUtils.cosineSimilarity(embeddings.get(i), lineEmb);
                            if (score > bestScore) {
                                bestScore = score;
                                bestKey = key;
                                bestFragment = fragments.get(i);
                            }
                        }
                    }

                    if (bestScore > SIMILARITY_THRESHOLD && !result.containsKey(bestKey)) {
                        List<String> indicators = TextSimilarityUtils.extractCommonIndicators(cleaned, bestFragment);
                        currentMatchResults.add(new MatchResult(cleaned, bestKey, bestFragment, bestScore, indicators));
                        result.put(bestKey, cleaned);
                        totalScore += bestScore;
                    }
                }

                if (!"insider".equals(sender)) {
                    processedTemplates++;
                    sendProgress(processedTemplates, totalTemplates, sender);
                }

                if (totalScore > highestScore) {
                    highestScore = totalScore;
                    bestResult = result;
                    bestTemplateName = fileName;
                    bestJsonModel = cachedTemplate.getFragments();
                    matchStats.add(new MatchMeta(bestTemplateName, totalScore, result.size()));
                    bestJsonMatchResult.put(bestTemplateName, currentMatchResults);
                }
            }

            ObjectNode wrapper = buildFinalJson(bestResult, bestJsonModel, doc, bestTemplateName);
            JsonNode bestJsonNode = mapper.valueToTree(bestJsonMatchResult);
            JsonNode matchStatsNode = mapper.valueToTree(matchStats);

            redisService.saveData("bestJsonNode:" + doc, bestJsonNode.toString());
            redisService.saveData("matchStatsNode:" + doc, matchStatsNode.toString());
            redisService.saveData(doc, wrapper.toString());

            if (!"insider".equals(sender)) {
                kafkaProducerService.sendMessage("after-analysis", new Message(sender, "/queue/result", doc).getJson());
            }

        } catch (Exception e) {
            logger.error("\uD83D\uDEA8 Помилка аналізу документа '{}': {}", doc, e.getMessage(), e);
        }
    }

    /**
     * Надсилає прогрес обробки шаблонів через Kafka
     * @param processedTemplates кількість оброблених шаблонів
     * @param totalTemplates загальна кількість шаблонів
     * @param sender ідентифікатор відправника
     */
    private void sendProgress(int processedTemplates, int totalTemplates, String sender) {
        int progressPercent = (int) ((processedTemplates / (double) totalTemplates) * 100);
        if (progressPercent != lastSentPercent) {
            kafkaProducerService.sendMessage("after-analysis",
                    new Message(sender, "/queue/status", progressPercent + "%").getJson());
            lastSentPercent = progressPercent;
        }
    }

    /**
     * Створює фінальний JSON-об'єкт з полями документа, що були знайдені, шаблоном і назвою документа
     *
     * @param bestResult знайдені відповідності (ключ — поле, значення — рядок з документа)
     * @param bestJsonModel оригінальна структура шаблону
     * @param doc назва документа
     * @param bestTemplateName ім'я найкращого шаблону
     * @return об'єкт JSON, який буде збережено і передано
     */
    private ObjectNode buildFinalJson(Map<String, String> bestResult,
                                      Map<String, List<String>> bestJsonModel,
                                      String doc,
                                      String bestTemplateName) {
        ObjectNode wrapper = mapper.createObjectNode();

        if (bestResult == null || bestResult.isEmpty()) {
            wrapper.put("status", "not found");
            return wrapper;
        }

        ObjectNode document = mapper.createObjectNode();
        bestResult.forEach(document::put);

        String expectedTitle = Optional.ofNullable(bestJsonModel)
                .map(map -> map.get("title"))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .filter(title -> !title.isBlank())
                .orElse("not_title");

        if (!document.has("title") || document.get("title").asText().isBlank()) {
            boolean foundInContent = bestResult.values().stream().anyMatch(v -> v.contains(expectedTitle));
            document.put("title", foundInContent ? expectedTitle : "not_title");
        }

        wrapper.put("doc", doc);
        wrapper.put("template", bestTemplateName);
        wrapper.set("document", document);
        return wrapper;
    }
}
