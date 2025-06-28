package org.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

/**
 * KafkaProducerService — сервіс для відправлення повідомлень у Kafka.
 * <p>
 * Цей клас використовується для синхронної передачі повідомлень у задані Kafka-топіки.
 * Повідомлення надсилаються як рядки (String).
 */
@Service
public class KafkaProducerService {

    /**
     * Логер для виводу інформації, попереджень та помилок у консоль або лог-файл.
     */
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    /**
     * KafkaTemplate — основний інструмент для взаємодії з Kafka.
     * У даному випадку використовується шаблон для пар <String, String>,
     * де ключ і значення — це рядки.
     */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Конструктор, через який Spring автоматично впроваджує KafkaTemplate.
     *
     * @param kafkaTemplate інструмент для надсилання повідомлень у Kafka
     */
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Надсилає повідомлення у вказаний Kafka-топік.
     * Метод є синхронним — чекає, поки Kafka підтвердить надсилання.
     *
     * @param topic   назва Kafka-топіка, куди буде надіслано повідомлення
     * @param message повідомлення у вигляді JSON або тексту
     * @return true, якщо повідомлення було успішно надіслано; false — у разі помилки
     */
    public void sendMessage(String topic, String message) {
        try {
            // Синхронне надсилання: чекаємо, поки Kafka завершить обробку
            kafkaTemplate.send(topic, message).get();
        } catch (ExecutionException | InterruptedException e) {
            // Логування винятку у разі проблем з надсиланням
            logger.error("Kafka send error: {}", e.getMessage(), e);
        }
    }
}
