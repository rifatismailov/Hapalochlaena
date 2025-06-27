package org.example.service;

import ai.djl.inference.Predictor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.Message;
import org.example.kafka.KafkaProducerService;
import org.example.loader.ModelLoader;
import org.example.redis.RedisService;
import org.example.service.match.MatchMeta;
import org.example.service.match.MatchResult;
import org.example.untils.TextSimilarityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MatcherServiceAsync {
    private static final Logger logger = LoggerFactory.getLogger(MatcherServiceAsync.class);
    private static final double SIMILARITY_THRESHOLD = 0.75;
    private final ModelLoader modelLoader;
    private final RedisService redisService;
    private final TemplateCache templateCache;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private KafkaProducerService kafkaProducerService;

    public MatcherServiceAsync(ModelLoader modelLoader, RedisService redisService, TemplateCache templateCache) {
        this.modelLoader = modelLoader;
        this.redisService = redisService;
        this.templateCache = templateCache;
    }

    public void matchDocument(String sender, String doc, List<String> lines) {
        try {
            Predictor<String, float[]> predictor = modelLoader.newPredictor();
            double highestScore = -1;
            String bestTemplateName = null;
            Map<String, String> bestResult = null;
            Map<String, String> bestJsonModel = null;
            Map<String, List<MatchResult>> bestJsonMatchResult = new HashMap<>();
            List<MatchMeta> matchStats = new ArrayList<>();

            Map<String, Map<String, String>> allTemplates = templateCache.getTemplates();
            int totalTemplates = allTemplates.size();
            int processedTemplates = 0;

            for (Map.Entry<String, Map<String, String>> entry : allTemplates.entrySet()) {
                String fileName = entry.getKey();
                Map<String, String> jsonModel = entry.getValue();

                Map<String, List<String>> templateFragments = new HashMap<>();
                Map<String, List<float[]>> templateEmbeddings = new HashMap<>();

                for (var e : jsonModel.entrySet()) {
                    List<String> fragments = List.of(e.getValue().split("[.!?\\n]"));
                    List<float[]> embeddings = new ArrayList<>();
                    for (String frag : fragments) {
                        frag = frag.trim();
                        if (!frag.isEmpty()) embeddings.add(predictor.predict(frag));
                    }
                    templateFragments.put(e.getKey(), fragments);
                    templateEmbeddings.put(e.getKey(), embeddings);
                }

                Map<String, String> result = new LinkedHashMap<>();
                double totalScore = 0.0;
                List<MatchResult> currentMatchResults = new ArrayList<>();

                for (String line : lines) {
                    String cleaned = line.replaceAll("\\s+", " ").trim();
                    if (cleaned.isBlank()) continue;
                    float[] lineEmb = predictor.predict(cleaned);

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
                    bestJsonModel = jsonModel;
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
                //messagingTemplate.convertAndSendToUser(sender, "/queue/result", doc);
                kafkaProducerService.sendMessage("after-analysis", new Message(sender,"/queue/result",doc).getJson());
            }

        } catch (Exception e) {
            logger.error("❌ Помилка аналізу документа '{}': {}", doc, e.getMessage(), e);
        }
    }

    private int lastSentPercent = -1;

    private void sendProgress(int processedTemplates, int totalTemplates, String sender) {
        int progressPercent = (int) ((processedTemplates / (double) totalTemplates) * 100);
        if (progressPercent != lastSentPercent) {
            //messagingTemplate.convertAndSendToUser(sender, "/queue/status", progressPercent + "%");
            kafkaProducerService.sendMessage("after-analysis", new Message(sender,"/queue/status",progressPercent + "%").getJson());

            lastSentPercent = progressPercent;
        }
    }


    private ObjectNode buildFinalJson(Map<String, String> bestResult, Map<String, String> bestJsonModel,
                                      String doc, String bestTemplateName) {
        ObjectNode wrapper = mapper.createObjectNode();
        if (bestResult != null) {
            ObjectNode document = mapper.createObjectNode();
            for (var e : bestResult.entrySet()) {
                document.put(e.getKey(), e.getValue());
            }

            String expectedTitle = Optional.ofNullable(bestJsonModel)
                    .map(map -> map.get("title"))
                    .filter(t -> !t.isBlank())
                    .orElse("not_title");

            if (!document.has("title") || document.get("title").asText().isBlank()) {
                for (String e : bestResult.values()) {
                    if (e.contains(expectedTitle)) {
                        document.put("title", expectedTitle);
                        break;
                    }
                }
                if (!document.has("title")) document.put("title", expectedTitle);
            }

            wrapper.put("doc", doc);
            wrapper.put("template", bestTemplateName);
            wrapper.set("document", document);
        } else {
            wrapper.put("status", "not found");
        }
        return wrapper;
    }
}
