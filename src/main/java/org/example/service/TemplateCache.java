package org.example.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.*;
import java.util.*;

@Component
public class TemplateCache {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCache.class);
    @Getter
    private final Map<String, Map<String, String>> templates = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Path TEMPLATE_DIR = Path.of("templates/model/");

    @PostConstruct
    public void loadTemplates() {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(TEMPLATE_DIR, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                Map<String, String> jsonModel = mapper.readValue(file.toFile(), Map.class);
                templates.put(fileName, jsonModel);
            }
            logger.info("✅ Завантажено {} шаблонів у кеш", templates.size());
        } catch (Exception e) {
            logger.error("❌ Помилка завантаження шаблонів у кеш: {}", e.getMessage(), e);
        }
    }

}

