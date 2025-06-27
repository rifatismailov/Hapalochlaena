package test;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class KafkaClient {

    public static void main(String[] args) {
        // Створення RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // URL контролера Kafka
        String url = "http://localhost:8080/api/kafka/send";

        // Повідомлення яке хочеш надіслати
        String message = "Привіт, Kafka!";

        // Налаштування заголовків
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Створення тіла запиту
        HttpEntity<String> request = new HttpEntity<>(message, headers);

        // Відправлення POST-запиту
        String response = restTemplate.postForObject(url, request, String.class);

        System.out.println("📨 Відповідь від сервера: " + response);
    }
}

