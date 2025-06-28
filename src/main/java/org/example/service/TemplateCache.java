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

/**
 * TemplateCache — компонент для кешування шаблонів документів.
 * <p>
 * При старті застосунку виконує завантаження всіх JSON-файлів із каталогу `templates/model/`
 * у пам'ять (у вигляді мапи), щоб уникнути повторного читання з диска під час виконання.
 */
@Component
public class TemplateCache {

    /**
     * Логер для запису інформаційних повідомлень та помилок у консоль/лог-файл.
     */
    private static final Logger logger = LoggerFactory.getLogger(TemplateCache.class);

    /**
     * Кешовані шаблони.
     * Зовнішня мапа: назва файлу → мапа полів шаблону (ключ-значення).
     * Наприклад: "nakaz.json" → { "title": "наказ", "organization": "..." }
     */
    @Getter
    private final Map<String, Map<String, String>> templates = new HashMap<>();

    /**
     * Jackson ObjectMapper — використовується для парсингу JSON-файлів у Map.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Шлях до директорії, де зберігаються шаблони у форматі JSON.
     */
    private static final Path TEMPLATE_DIR = Path.of("templates/model/");

    /**
     * Метод автоматично викликається після створення біну (через @PostConstruct).
     * Завантажує всі JSON-файли з папки `templates/model/` у памʼять у вигляді мапи.
     */
    @PostConstruct
    public void loadTemplates() {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(TEMPLATE_DIR, "*.json")) {
            for (Path file : files) {
                String fileName = file.getFileName().toString(); // приклад: "nakaz.json"

                // Зчитуємо JSON-файл як Map<String, String>
                Map<String, String> jsonModel = mapper.readValue(file.toFile(), Map.class);

                // Додаємо у кеш
                templates.put(fileName, jsonModel);
            }

            logger.info("Завантажено {} шаблонів у кеш", templates.size());
        } catch (Exception e) {
            logger.error("Помилка завантаження шаблонів у кеш: {}", e.getMessage(), e);
        }
    }
}
