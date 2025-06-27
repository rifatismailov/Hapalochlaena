
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>DocumentControllerMockMvcTest</h2>
 * <p>
 *  Integration-test для REST-контролера <code>/api/match</code>.
 *  Тест читає всі текстові файли з каталогу <code>data/documents</code>,
 *  відправляє кожен документ у вигляді plain-text POST-запиту й перевіряє,
 *  що сервіс повертає JSON з коректно визначеним заголовком<br/>
 *  (<code>$.document.title</code>) та шаблоном (<code>template</code>).
 * </p>
 * <p>
 *  Додатково відповідь логуються у людино-читабельному (pretty-printed) форматі.
 * </p>
 */

@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(classes = org.example.BertMatcherApplication.class)

public class DocumentControllerMockMvcTest {

    /**
     * <p>Інжектований <b>MockMvc</b> з контексту Spring Boot.</p>
     * <ul>
     *     <li>Емітує HTTP-запити до вбудованого контексту без підняття реального сервера.</li>
     *     <li>Дозволяє очікувати статус, заголовки та тіло відповіді.</li>
     * </ul>
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * <p>Один спільний інстанс {@link ObjectMapper} для парсингу JSON-відповідей.</p>
     * Використовується <i>pretty printer</i> для форматованого виводу у консоль.
     */
    private final ObjectMapper mapper = new ObjectMapper();


    /**
     * Основний тест.
     * <ol>
     *     <li>Знаходить усі файли в каталозі <code>data/documents</code>.</li>
     *     <li>Для кожного файлу викликає {@link #replaceText(String)} – прибирає зайві переноси.</li>
     *     <li>Надсилає POST-запит <code>/api/match</code> у кодуванні UTF-8.</li>
     *     <li>Перевіряє статус <code>200 OK</code> та наявність поля <code>$.document.title</code>.</li>
     *     <li>Парсить тіло відповіді у {@link JsonNode} та логує <code>template</code> і <code>title</code>.</li>
     * </ol>
     *
     * @throws Exception будь-які помилки IO або MockMvc
     */
    private final File folder = new File("data/documents");
    private final File[] files = folder.listFiles();

//    @Test
//    public void testDocumentWithRequestMatchingSync() throws Exception {
//
//
//        if (files == null || files.length == 0) {
//            System.out.println("⚠️  Файли не знайдено в директорії: data/documents");
//            return;
//        }
//
//        for (File file : files) {
//            if (!file.isFile()) continue;
//
//            String rawText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
//            String preparedText = replaceText(rawText);
//
//            // 🔧 Створюємо JSON для DocRequest
//            Map<String, String> docRequest = new HashMap<>();
//            docRequest.put("doc", file.getName());
//            docRequest.put("body", preparedText);
//            String jsonBody = mapper.writeValueAsString(docRequest);
//
//            MvcResult result = mockMvc.perform(post("/api/match/sync")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .characterEncoding("UTF-8")
//                            .content(jsonBody))
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andReturn();
//
//            // 📦 Парсимо відповідь
//            String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
//            JsonNode jsonNode = mapper.readTree(response);
//
//            String title = jsonNode.at("/document/title").asText("🔴 not_found");
//            String template = jsonNode.path("template").asText("🔴 not_found");
//
//            System.out.println("📝 Файл: " + file.getName());
//            System.out.println("   ▶ Template: " + template);
//            System.out.println("   ▶ Title   : " + title);
//            System.out.println("   ▶ JSON pretty:\n" +
//                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
//            System.out.println("------------------------------------------------------\n");
//        }
//    }
//
//    @Test
//    public void testDocumentWithRequestMatchingAsync() throws Exception {
//
//        if (files == null || files.length == 0) {
//            System.out.println("⚠️  Файли не знайдено в директорії: data/documents");
//            return;
//        }
//        for (File file : files) {
//            MvcResult result = mockMvc.perform(delete("/api/redis/delete")
//                            .param("key", file.getName())
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isOk())
//                    .andReturn();
//
//            String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
//            System.out.println("🗑️ Результат видалення з Redis: " + response);
//
//
//            // Якщо значення має бути JSON — можна розпарсити:
//            // JsonNode json = new ObjectMapper().readTree(response);
//            // System.out.println("▶ Поле document.title: " + json.at("/document/title").asText());
//        }
//        for (File file : files) {
//            if (!file.isFile()) continue;
//
//            String rawText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
//            String preparedText = replaceText(rawText);
//
//            // 🔧 Створюємо JSON для DocRequest
//            Map<String, String> docRequest = new HashMap<>();
//            docRequest.put("doc", file.getName());
//            docRequest.put("body", preparedText);
//            String jsonBody = mapper.writeValueAsString(docRequest);
//
//            MvcResult result = mockMvc.perform(post("/api/match/async")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .characterEncoding("UTF-8")
//                            .content(jsonBody))
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andReturn();
//
//            // 📦 Парсимо відповідь
//            String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
//            System.out.println("   ▶ response   : " + response);
//            System.out.println("------------------------------------------------------\n");
//        }
//    }
//
//
//    @Test
//    public void testDocumentWithRequestMatchingGet() throws Exception {
//
//
//        if (files == null || files.length == 0) {
//            System.out.println("⚠️  Файли не знайдено в директорії: data/documents");
//            return;
//        }
//
//        for (File file : files) {
//            MvcResult result = mockMvc.perform(get("/api/redis/get")
//                            .param("key", file.getName())
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andReturn(); // не .andExpect(status().isOk())
//
//            int status = result.getResponse().getStatus();
//            String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
//
//            if (status == HttpStatus.OK.value()) {
//                JsonNode jsonNode = mapper.readTree(response);
//
//                String title = jsonNode.at("/document/title").asText("🔴 not_found");
//                String template = jsonNode.path("template").asText("🔴 not_found");
//
//                System.out.println("📝 Файл: " + file.getName());
//                System.out.println("   ▶ Template: " + template);
//                System.out.println("   ▶ Title   : " + title);
//                System.out.println("   ▶ JSON pretty:\n" +
//                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
//                System.out.println("------------------------------------------------------\n");
//            } else if (status == HttpStatus.NOT_FOUND.value()) {
//                System.out.println("⚠️ Ключ не знайдено в Redis: " + response);
//            } else {
//                System.out.println("❌ Невідомий статус: " + status + " → " + response);
//            }
//
//            // Якщо значення має бути JSON — можна розпарсити:
//            // JsonNode json = new ObjectMapper().readTree(response);
//            // System.out.println("▶ Поле document.title: " + json.at("/document/title").asText());
//        }
//    }

        @Test
    public void testDocumentWithRequestMatchingAsync() throws Exception {

        if (files == null || files.length == 0) {
            System.out.println("⚠️  Файли не знайдено в директорії: data/documents");
            return;
        }

        for (File file : files) {
            if (!file.isFile()) continue;

            String rawText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String preparedText = replaceText(rawText);

            // 🔧 Створюємо JSON для DocRequest
            Map<String, String> docRequest = new HashMap<>();
            docRequest.put("clientId", "insider");
            docRequest.put("doc", file.getName());
            docRequest.put("body", preparedText);
            String jsonBody = mapper.writeValueAsString(docRequest);
            // Створення RestTemplate
            RestTemplate restTemplate = new RestTemplate();

            // URL контролера Kafka
            String url = "http://localhost:8080/api/kafka/send";

            // Повідомлення яке хочеш надіслати

            // Налаштування заголовків
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Створення тіла запиту
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            // Відправлення POST-запиту
            String response = restTemplate.postForObject(url, request, String.class);

            System.out.println("📨 Відповідь від сервера: " + response);
        }
    }

    @Test
    public void testDocumentWithRequestMatchingKafka() throws Exception {

        if (files == null || files.length == 0) {
            System.out.println("⚠️  Файли не знайдено в директорії: data/documents");
            return;
        }
        for (File file : files) {
            String kafkaMessage = "Файл " + file.getName() + " оброблено";

            MvcResult result = mockMvc.perform(post("/api/kafka/send")
                            .content("\"" + kafkaMessage + "\"") // JSON string
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);


        }

    }
    /**
     * <p>Нормалізує текст документа:</p>
     * <ul>
     *     <li>рубрикує рядки, видаляючи зайві переноси;</li>
     *     <li>обʼєднує рядки, що починаються з малої літери або пунктуації, з попереднім рядком;</li>
     *     <li>залишає переноси лише перед рядками, що починаються з великої літери / цифри.</li>
     * </ul>
     *
     * @param rawText сирий текст із документа
     * @return нормалізований текст без «зламаних» переносів
     */
    public String replaceText(@NotNull String rawText) {
        StringBuilder result = new StringBuilder();
        String[] rawLines = rawText.split("\\R+"); // усі типи переносу рядка

        for (String rawLine : rawLines) {
            String current = rawLine.trim();
            if (current.isEmpty()) continue;
            char firstChar = current.charAt(0);

            if (!result.isEmpty()) {
                // якщо рядок продовжує попередній абзац
                if (Character.isLowerCase(firstChar) || ".;:".indexOf(firstChar) != -1) {
                    result.append(' ').append(current);
                } else {
                    result.append('\n').append(current);
                }
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}
