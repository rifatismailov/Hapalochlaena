package org.example.analysis;

import org.example.ErrorResponse;
import org.example.controller.DocumentController;
import org.example.redis.RedisService;
import org.example.service.MatcherServiceAsync;
import org.example.untils.DocRequest;
import org.example.untils.DocRequestUtils;
import org.example.untils.JsonSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final MatcherServiceAsync matcherServiceAsync;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RedisService redisService;

    public DocumentAnalysisLauncher(@Qualifier("taskExecutor")ThreadPoolTaskExecutor taskExecutor,
                              RedisService redisService,
                              MatcherServiceAsync matcherServiceAsync) {
        this.taskExecutor = taskExecutor;
        this.redisService = redisService;
        this.matcherServiceAsync = matcherServiceAsync;
    }

    public ErrorResponse addTaskAsync(DocRequest request) {
        try {
            if (taskExecutor.getActiveCount() >= taskExecutor.getMaxPoolSize()) {
                logger.warn("Потоки зайняті. Додаємо в Redis: {}", request.getDoc());
                String jsonBody = DocRequestUtils.createJsonBody(request);
                redisService.addToLine("requestQueue", jsonBody);
            } else {
                logger.info("▶️ Починаємо обробку документа: {}", request.getDoc());
                submitTask(request);
            }
        } catch (Exception e) {
            logger.error("Помилка всередині matchAsync: {}", e.getMessage(), e);
            return new ErrorResponse(e.getMessage(), 500);
        }
        return null;
    }

    /**
     * Надсилання задачі в пул потоків (із захистом).
     */
    private void submitTask(DocRequest request) {
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
            logger.warn("⚠️ Потік відхилено. Ставимо в Redis: {}", request.getDoc());
            String jsonBody = DocRequestUtils.createJsonBody(request);
            redisService.addToLine("requestQueue", jsonBody);
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
                    logger.error("❌ Не вдалося обробити повідомлення з Redis: {}", e.getMessage(), e);
                }
            }
        }
    }
}
