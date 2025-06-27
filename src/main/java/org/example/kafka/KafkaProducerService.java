package org.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean sendMessage(String topic, String message) {
        try {
            // Використовуємо .get() для блокування, поки повідомлення не буде надіслано
            kafkaTemplate.send(topic, message).get();
            return true;  // Повертаємо true, якщо повідомлення відправлено успішно
        } catch (ExecutionException | InterruptedException e) {
            // Логування помилки та повернення false, якщо сталася помилка
            logger.error("Kafka send error: {}", e.getMessage(), e);
            return false;  // Повертаємо false, якщо сталася помилка
        }
    }

}
