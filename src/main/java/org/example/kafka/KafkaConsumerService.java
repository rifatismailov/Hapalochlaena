package org.example.kafka;

import org.example.untils.Response;
import org.example.untils.Message;
import org.example.analysis.DocumentAnalysisLauncher;
import org.example.untils.DocRequest;
import org.example.untils.JsonSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Сервіс KafkaConsumerService відповідає за прослуховування Kafka-топіку "analysis"
 * та обробку повідомлень, які надходять від інших мікросервісів або клієнтів.
 * <p>
 * Основна функція цього класу:
 * - Отримати JSON-повідомлення з Kafka.
 * - Перетворити його у об'єкт DocRequest.
 * - Запустити аналіз документа через DocumentAnalysisLauncher.
 * - Надіслати результат назад у топік "after-analysis" через KafkaProducerService.
 */
@Service
public class KafkaConsumerService {

    /**
     * Логер для виводу інформації, попереджень та помилок у консоль або лог-файл.
     */
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    /**
     * Сервіс, який виконує асинхронний аналіз документа.
     * Впроваджується через Spring (Autowired).
     */
    @Autowired
    private DocumentAnalysisLauncher documentAnalysisLauncher;

    /**
     * Сервіс, який відповідає за надсилання повідомлень до Kafka.
     * Використовується для передачі результатів аналізу назад.
     */
    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * Метод слухає Kafka-топік "analysis" у групі "hapalochlaena".
     * При надходженні нового повідомлення:
     * - Логуються його розмір (довжина).
     * - Воно десеріалізується у DocRequest.
     * - Проводиться аналіз документа.
     * - Результат (у вигляді ErrorResponse) надсилається у Kafka топік "after-analysis".
     *
     * @param message JSON-рядок, який представляє собою DocRequest
     */
    @KafkaListener(topics = "analysis", groupId = "hapalochlaena")
    public void listen(String message) {
        try {
            logger.info("Message length for analysis: {}", message.length());

            // Преобразуємо JSON у DocRequest
            DocRequest docRequest = JsonSerializable.fromJson(message, DocRequest.class);

            // Викликаємо асинхронний аналіз
            Response response = documentAnalysisLauncher.addTaskAsync(docRequest);

            // Якщо результат не порожній, відправляємо назад
            if (response != null) {
                kafkaProducerService.sendMessage(
                        "after-analysis",
                        new Message(
                                docRequest.getClientId(),          // Ідентифікатор клієнта
                                "/queue/result",                   // Канал, куди надсилати відповідь
                                response.getJson()            // Результат отриманої помилки у форматі JSON якщо воно відбудеться
                        ).getJson()
                );
            }

        } catch (Exception e) {
            // У випадку помилки логуємо її з повним трасуванням
            logger.error("An error occurred inside the analysis method: {}", e.getMessage(), e);
        }
    }
}
