package org.example.kafka;

import org.example.ErrorResponse;
import org.example.Message;
import org.example.analysis.DocumentAnalysisLauncher;
import org.example.untils.DocRequest;
import org.example.untils.JsonSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private DocumentAnalysisLauncher documentAnalysisLauncher;
    @Autowired
    private KafkaProducerService kafkaProducerService;

    @KafkaListener(topics = "analysis", groupId = "hapalochlaena")
    public void listen(String message) {
        try {
            logger.info("Message length for analysis: {}", message.length());
            // Преобразуємо отримане повідомлення у DocRequest
            DocRequest docRequest = JsonSerializable.fromJson(message, DocRequest.class);
            // Викликаємо метод matchSync з сервісу
            ErrorResponse errorResponse = documentAnalysisLauncher.addTaskAsync(docRequest);
            if(errorResponse!=null){
            kafkaProducerService.sendMessage("after-analysis",
                    new Message(docRequest.getClientId(), "/queue/result", errorResponse.getJson()).getJson());
            }

        } catch (Exception e) {
            logger.error("An error occurred inside the analysis method: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "web-analysis", groupId = "hapalochlaena")
    public void listen_to(String message) {
        try {
            logger.info("Message length for after_analysis: {}", message.length());
            // Преобразуємо отримане повідомлення у DocRequest
            DocRequest docRequest = JsonSerializable.fromJson(message, DocRequest.class);
            // Викликаємо метод matchSync з сервісу
            ErrorResponse errorResponse = documentAnalysisLauncher.addTaskAsync(docRequest);
            if(errorResponse!=null) {
                kafkaProducerService.sendMessage("after-analysis",
                        new Message(docRequest.getClientId(), "/queue/result", errorResponse.getJson()).getJson());
            }
        } catch (Exception e) {
            logger.error("An error occurred inside the after_analysis method: {}", e.getMessage(), e);
        }
    }
}
