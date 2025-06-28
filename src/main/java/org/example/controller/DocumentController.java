package org.example.controller;

import org.example.untils.Response;
import org.example.analysis.DocumentAnalysisLauncher;
import org.example.untils.DocRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match")
public class DocumentController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    @Autowired
    private DocumentAnalysisLauncher documentAnalysisLauncher;
    /**
     * Аналізує вміст документа, розбитий на рядки.
     *
     * @param docRequest Обʼєкт з текстом документа
     * @return JSON-результат або JSON-помилка
     * асинхронна обработка
     * <p>
     * Асинхронний аналіз документа.
     * Якщо потоків немає — ставимо в Redis чергу.
     */
    @PostMapping(value = "/async", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> matchAsync(@RequestBody DocRequest docRequest) {
        try {
            // Викликаємо метод matchSync з сервісу
            Response response = documentAnalysisLauncher.addTaskAsync(docRequest);
            if (response != null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("Document processing has begun, the result will be sent later.");
            } else {
                return ResponseEntity.status(500)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response.getJson());
            }
        } catch (Exception e) {
            logger.error("❌ Помилка всередині matchAsync: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Error occurred: " + e.getMessage());
        }
    }
}
