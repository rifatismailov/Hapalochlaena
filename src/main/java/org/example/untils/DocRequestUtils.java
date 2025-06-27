package org.example.untils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Утилітний клас для формування JSON-повідомлень для DocRequest.
 */
public class DocRequestUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Створює JSON-представлення запиту з параметрами.
     *
     * @param clientId ідентифікатор клієнта
     * @param fileName назва документа
     * @param body     вміст документа
     * @return JSON-рядок
     * @throws RuntimeException якщо не вдається створити JSON
     */
    public static String createJsonBody(String clientId, String fileName, String body) {
        Map<String, Object> docRequest = new HashMap<>();
        docRequest.put("clientId", clientId);
        docRequest.put("doc", fileName);
        docRequest.put("body", body);

        try {
            return mapper.writeValueAsString(docRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не вдалося сформувати JSON для DocRequest", e);
        }
    }

    /**
     * Створює JSON-представлення з об'єкта DocRequest.
     */
    public static String createJsonBody(DocRequest request) {
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не вдалося сформувати JSON з DocRequest", e);
        }
    }
}
