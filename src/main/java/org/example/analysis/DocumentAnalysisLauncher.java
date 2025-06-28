package org.example.analysis;

import org.example.untils.Response;
import org.example.untils.Message;
import org.example.kafka.KafkaProducerService;
import org.example.redis.RedisService;
import org.example.service.MatcherServiceAsync;
import org.example.untils.DocRequest;
import org.example.untils.DocRequestUtils;
import org.example.untils.JsonSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.concurrent.RejectedExecutionException;


/**
 * Сервіс для обробки документів, отриманих від клієнтів, та передачі їх на аналіз.
 */
@Service
public class DocumentAnalysisLauncher {
    private static final Logger logger = LoggerFactory.getLogger(DocumentAnalysisLauncher.class);
    @Autowired
    private KafkaProducerService kafkaProducerService;

    private final MatcherServiceAsync matcherServiceAsync;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RedisService redisService;

    public DocumentAnalysisLauncher(@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
                                    RedisService redisService,
                                    MatcherServiceAsync matcherServiceAsync) {
        this.taskExecutor = taskExecutor;
        this.redisService = redisService;
        this.matcherServiceAsync = matcherServiceAsync;
    }

    public Response addTaskAsync(DocRequest request) {
        try {
            if (taskExecutor.getActiveCount() >= taskExecutor.getMaxPoolSize()) {
                logger.warn("Потоки зайняті. Додаємо в Redis: {}", request.getDoc());
                String jsonBody = DocRequestUtils.createJsonBody(request);
                redisService.addToLine("requestQueue", jsonBody);
                sendInfo(request.getClientId(), "Сервіс обробляє інші документи. Ваш " + request.getDoc() + " у черзі. Зачекайте.");

            } else {
                logger.info("▶️ Починаємо обробку документа: {}", request.getDoc());
                submitTask(request);
            }
        } catch (Exception e) {
            logger.error("Помилка всередині matchAsync: {}", e.getMessage(), e);
            return new Response(e.getMessage(), 500);
        }
        return null;
    }

    /**
     * Надсилання задачі в пул потоків (із захистом).
     */
    private void submitTask(DocRequest request) {
        int activeThreads = taskExecutor.getActiveCount();
        int maxThreads = taskExecutor.getMaxPoolSize(); // або corePoolSize, якщо використовуєш лише core
        System.out.println(activeThreads+" "+maxThreads);
        if (activeThreads >= maxThreads) {
            // Усі потоки зайняті — документ у Redis
            logger.info("🕒 Всі потоки зайняті ({} з {}). Ставимо в Redis: {}", activeThreads, maxThreads, request.getDoc());
            String jsonBody = DocRequestUtils.createJsonBody(request);
            redisService.addToLine("requestQueue", jsonBody);
            sendInfo(request.getClientId(), "Сервіс обробляє інші документи. Ваш " + request.getDoc() + " у черзі. Зачекайте.");
            return;
        }

        // Є вільні потоки — виконуємо одразу
        try {
            taskExecutor.execute(() -> {
                logger.info("🔧 Обробка документа: {}", request.getDoc());
                matcherServiceAsync.matchDocument(
                        request.getClientId(),
                        request.getDoc(),
                        Arrays.asList(request.getBody().split("\n"))
                );
            });
        } catch (RejectedExecutionException ex) {
            // На випадок, якщо навіть при активних потоках щось пішло не так
            logger.warn("⚠️ Виняток при виконанні taskExecutor. Ставимо в Redis: {}", request.getDoc());
            String jsonBody = DocRequestUtils.createJsonBody(request);
            redisService.addToLine("requestQueue", jsonBody);
            sendInfo(request.getClientId(), "Обробник зайнятий. Ваш документ " + request.getDoc() + " в черзі. Чекайте.");
        }
    }


    private void sendInfo(String user, String message) {
        if (!"insider".equals(user)) {
            kafkaProducerService.sendMessage("after-analysis", new Message(
                    user,
                    "/queue/status",
                    message
            ).getJson());
        }
    }

    /**
     * Перевірка Redis черги — викликається кожні 3 секунди.
     * Працює тільки якщо є вільні потоки.
     */
    @Scheduled(fixedDelay = 3000)
    public void pullFromRedisQueue() {
        if (taskExecutor.getActiveCount() < taskExecutor.getMaxPoolSize()) {
            String nextMessage = redisService.getOnLine("requestQueue");
            if (nextMessage != null) {
                try {
                    DocRequest docRequest = JsonSerializable.fromJson(nextMessage, DocRequest.class);
                    logger.info("📦 Витягнуто з Redis черги: {}", docRequest.getDoc());
                    submitTask(docRequest);
                } catch (Exception e) {
                    logger.error("Не вдалося обробити повідомлення з Redis: {}", e.getMessage(), e);
                }
            }
        }
    }
}
