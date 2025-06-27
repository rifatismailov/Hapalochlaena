/**
 * MatcherService — сервіс для семантичного порівняння текстів документів із шаблонами.
 * <p>
 * Основне призначення:
 * - Аналізує вхідний документ (у вигляді списку рядків)
 * - Порівнює текст з шаблонами, що зберігаються в JSON
 * - Повертає шаблон, який найбільше відповідає документу
 * - Показує індикатори (спільні слова), які вплинули на збіг
 * <p>
 * Технології:
 * - Apache DJL (Deep Java Library) для генерації embedding-векторів
 * - Jackson ObjectMapper для обробки JSON
 * - Використовує cosine similarity для порівняння
 */
package org.example.service;

import ai.djl.inference.Predictor;
import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.service.match.MatchMeta;
import org.example.service.match.MatchResult;
import org.example.loader.ModelLoader;
import org.example.untils.TextSimilarityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class MatcherServiceSync {

    /**
     * Завантажувач нейромережі
     */
    private final ModelLoader modelLoader;

    /**
     * Jackson mapper для роботи з JSON
     */
    private final ObjectMapper mapper = new ObjectMapper();

    public MatcherServiceSync(ModelLoader modelLoader) {
        this.modelLoader = modelLoader;
    }

    /**
     * Основний метод, який здійснює порівняння документа з шаблонами.
     *
     * @param lines Рядки документа
     * @return JSON-об'єкт з найбільш відповідним шаблоном та заповненими полями
     * @throws IOException        якщо не вдається прочитати шаблон
     * @throws TranslateException якщо виникла помилка в моделі
     */
    public ObjectNode matchDocument(String doc,List<String> lines) throws IOException, TranslateException {
        System.out.println("MatcherService");
        Path templateDir = Path.of("templates/model/");
        Predictor<String, float[]> predictor = modelLoader.newPredictor();
        double highestScore = -1;
        String bestTemplateName = null;
        Map<String, String> bestResult = null;
        Map<String, String> bestJsonModel = null;
        Map<String, List<MatchResult>> bestJsonMatchResult = new HashMap<>();
        List<MatchMeta> matchStats = new ArrayList<>();


        // Відкриваємо директорію з JSON-файлами шаблонів
        try (DirectoryStream<Path> files = Files.newDirectoryStream(templateDir, "*.json")) {

            for (Path file : files) {

                // jsonModel: карта ключів шаблону до текстових значень (наприклад: "title" -> "НАКАЗ...")
                Map<String, String> jsonModel = mapper.readValue(file.toFile(), new TypeReference<>() {
                });

                // templateFragments: карта ключів шаблону до списків речень (фрагментів)
                // приклад: "orders_1" -> ["Здійснити запит...", "Надіслати копії..."]
                Map<String, List<String>> templateFragments = new HashMap<>();

                // templateEmbeddings: карта ключів шаблону до списків векторів (embedding-ів) кожного речення
                Map<String, List<float[]>> templateEmbeddings = new HashMap<>();

                // Обробка кожного ключа шаблону
                for (var entry : jsonModel.entrySet()) {
                    // Розбиваємо текст шаблону на речення (по ., !, ?, \n)
                    List<String> fragments = List.of(entry.getValue().split("[.!?\n]"));

                    // Список embedding-векторів для кожного речення
                    List<float[]> embeddings = new ArrayList<>();

                    for (String frag : fragments) {
                        frag = frag.trim();
                        if (!frag.isEmpty()) {
                            // Генеруємо embedding для кожного речення шаблону
                            embeddings.add(predictor.predict(frag));
                        }
                    }

                    // Зберігаємо фрагменти та їхні embedding-и
                    templateFragments.put(entry.getKey(), fragments);
                    templateEmbeddings.put(entry.getKey(), embeddings);
                }

                // result: збереження найкращих співпадінь ключ -> текст з документа
                Map<String, String> result = new LinkedHashMap<>();

                // Загальна сума схожості по цьому шаблону
                double totalScore = 0.0;
                int count = 0;
                // Перебір кожного рядка з документа
                List<MatchResult> currentMatchResults = new ArrayList<>();

                for (String line : lines) {
                    // cleaned: очищений рядок без зайвих пробілів
                    String cleaned = line.replaceAll("\\s+", " ").trim();
                    if (cleaned.isBlank()) continue;

                    // lineEmb: embedding рядка з документа
                    float[] lineEmb = predictor.predict(cleaned);

                    // bestKey: ключ шаблону, який найкраще збігся
                    String bestKey = null;

                    // bestFragment: конкретне речення шаблону, що дало найбільшу схожість
                    String bestFragment = null;

                    // bestScore: найвища cosine similarity
                    double bestScore = -1;
                    // Порівняння з усіма реченнями всіх ключів шаблону
                    for (var entry : templateEmbeddings.entrySet()) {
                        String key = entry.getKey();
                        List<float[]> embeddings = entry.getValue();
                        List<String> fragments = templateFragments.get(key);

                        for (int i = 0; i < embeddings.size(); i++) {
                            double score = TextSimilarityUtils.cosineSimilarity(embeddings.get(i), lineEmb);

                            // Якщо це найкращий результат — оновлюємо
                            if (score > bestScore) {
                                bestScore = score;
                                bestKey = key;
                                bestFragment = fragments.get(i);
                            }
                        }
                    }

                    // Зберігаємо тільки ті збіги, які мають високу схожість
                    if (bestScore > 0.75 && !result.containsKey(bestKey)) {
                        // indicators: список спільних слів між документом і шаблоном
                        List<String> indicators = TextSimilarityUtils.extractCommonIndicators(cleaned, bestFragment);

                        // Додованя результатів порівняння в консоль
                        MatchResult matchResult = new MatchResult(cleaned, bestKey, bestFragment, bestScore, indicators);
                        currentMatchResults.add(matchResult);
                        // Додаємо результат у підсумкову карту
                        result.put(bestKey, cleaned);
                        totalScore += bestScore;
                        count++;
                    }
                }

                // Якщо цей шаблон дав кращу схожість — зберігаємо його
                if (totalScore > highestScore) {
                    highestScore = totalScore;
                    bestResult = result;             // Найкращі відповідності ключів
                    bestTemplateName = file.getFileName().toString();  // Назва шаблону (JSON-файлу)
                    bestJsonModel = jsonModel;       // Сам шаблон
                    // Записуємо поточну статистику (навіть якщо не найкраща — якщо потрібно)
                    matchStats.add(new MatchMeta(
                            file.getFileName().toString(),  // template name
                            totalScore,                     // score
                            result.size()                   // кількість збігів (рядків)
                    ));

                    bestJsonMatchResult.put(file.getFileName().toString(), new ArrayList<>(currentMatchResults));
                    currentMatchResults.clear(); // Очищаємо список для наступного шаблону
                }

            }

        }

        // Формування фінального JSON
        ObjectNode wrapper = mapper.createObjectNode();
        if (bestResult != null) {
            ObjectNode document = mapper.createObjectNode();
            for (var e : bestResult.entrySet()) {
                document.put(e.getKey(), e.getValue());
            }

            // Перевірка наявності поля title
            String expectedTitle = Optional.ofNullable(bestJsonModel.get("title"))
                    .filter(t -> !t.isBlank())
                    .orElse("not_title");

            if (!document.has("title") || document.get("title").asText().isBlank()) {
                boolean found = false;
                for (String e : lines) {
                    if (e.contains(expectedTitle)) {
                        document.put("title", expectedTitle);
                        found = true;
                        break;
                    }
                }

                // Якщо не знайдено — просто додати як fallback
                if (!found) {
                    document.put("title", expectedTitle);
                }
            }

            JsonNode bestJsonNode = mapper.valueToTree(bestJsonMatchResult);
            JsonNode matchStatsNode = mapper.valueToTree(matchStats);
            wrapper.put("doc", doc);
            wrapper.put("template", bestTemplateName);
            wrapper.set("document", document);
            wrapper.set("matches", bestJsonNode);
            wrapper.set("stats", matchStatsNode);
           // System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper));
        } else {
            wrapper.put("status", "not found");
        }

        return wrapper;
    }

} 
