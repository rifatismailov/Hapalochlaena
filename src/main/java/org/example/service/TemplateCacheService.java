package org.example.service;

import ai.djl.inference.Predictor;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.example.loader.ModelLoader;
import org.example.redis.RedisService;
import org.example.untils.CachedTemplate;
import org.example.untils.JsonSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * TemplateCacheService — ініціалізує шаблони в памʼяті з Redis або з оригінального джерела (JSON),
 * та кешує їх у Redis для подальшого використання.
 */
@Service
public class TemplateCacheService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCacheService.class);

    private final Map<String, CachedTemplate> cachedTemplates = new HashMap<>();
    private final ModelLoader modelLoader;
    private final RedisService redisService;
    private final TemplateCache templateCache;
    @Getter
    private Predictor<String, float[]> predictor;


    public TemplateCacheService(ModelLoader modelLoader,
                                RedisService redisService,
                                TemplateCache templateCache) {
        this.modelLoader = modelLoader;
        this.redisService = redisService;
        this.templateCache = templateCache;
    }

    /**
     * Метод ініціалізує шаблони після запуску сервісу.
     * Якщо знайдено шаблони у Redis — вони десеріалізуються та використовуються.
     * Якщо Redis порожній — створюються заново та зберігаються в Redis.
     */
    @PostConstruct
    public void init() {
        try {
            predictor = modelLoader.newPredictor();
            // Перевіряємо наявність шаблонів у Redis
            String firstTemplate = redisService.getData("Templates-0");
            if (firstTemplate != null && !firstTemplate.isBlank() && !"null".equalsIgnoreCase(firstTemplate)) {
                logger.info("🔁 Завантаження шаблонів з Redis...");
                loadFromRedis();
            } else {
                logger.info("🆕 Кешування шаблонів з JSON...");
                buildAndCacheTemplates();
            }
        } catch (Exception e) {
            logger.error("Помилка під час ініціалізації шаблонів: {}", e.getMessage(), e);
        }
    }

    private void loadFromRedis() {
        int index = 0;
        String key;
        String data;
        do {
            key = "Templates-" + index;
            data = redisService.getData(key);
            if (data != null && !data.isBlank() && !"null".equalsIgnoreCase(data)) {
                CachedTemplate cachedTemplate = JsonSerializable.fromJson(data, CachedTemplate.class);
                cachedTemplates.put(key, cachedTemplate);
                logger.info("✅ Шаблон #{} завантажено: {} фрагментів", index, cachedTemplate.getEmbeddings().size());
                index++;
            }
        } while (data != null && !data.isBlank() && !"null".equalsIgnoreCase(data));
    }

    private void buildAndCacheTemplates() throws Exception {
        Map<String, Map<String, String>> allTemplates = templateCache.getTemplates();
        int index = 0;

        for (Map.Entry<String, Map<String, String>> entry : allTemplates.entrySet()) {
            String fileName = entry.getKey();
            Map<String, String> jsonModel = entry.getValue();

            // Пропускаємо, якщо шаблон уже в кеші
            if (cachedTemplates.containsKey(fileName)) continue;

            Map<String, List<String>> fragmentsMap = new HashMap<>();
            Map<String, List<float[]>> embeddingsMap = new HashMap<>();

            for (var e : jsonModel.entrySet()) {
                List<String> fragments = List.of(e.getValue().split("[.!?\\n]"));
                List<float[]> embeddings = new ArrayList<>();
                for (String fragment : fragments) {
                    fragment = fragment.trim();
                    if (!fragment.isEmpty()) {
                        embeddings.add(predictor.predict(fragment));
                    }
                }
                fragmentsMap.put(e.getKey(), fragments);
                embeddingsMap.put(e.getKey(), embeddings);
            }

            CachedTemplate cachedTemplate = new CachedTemplate(fragmentsMap, embeddingsMap);
            String redisKey = "Templates-" + index;
            cachedTemplates.put(redisKey, cachedTemplate);
            redisService.saveData(redisKey, cachedTemplate.getJson());
            logger.info("📦 Збережено шаблон у Redis: {}", redisKey);
            index++;
        }
    }

    public Map<String, CachedTemplate> getTemplates() {
        return cachedTemplates;
    }

    public CachedTemplate getTemplate(String templateName) {
        return cachedTemplates.get(templateName);
    }

    public void addTemplate(String templateName, CachedTemplate template) {
        cachedTemplates.put(templateName, template);
    }

    public boolean containsTemplate(String templateName) {
        return cachedTemplates.containsKey(templateName);
    }
}
