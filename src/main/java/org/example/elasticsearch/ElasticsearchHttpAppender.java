package org.example.elasticsearch;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

public class ElasticsearchHttpAppender extends AppenderBase<ILoggingEvent> {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchHttpAppender.class);

    @Override
    protected void append(ILoggingEvent event) {
        try {
            String url = "http://192.168.88.200:9200/logs/_doc";

            Map<String, Object> log = new HashMap<>();
            log.put("level", event.getLevel().toString());
            log.put("logger", event.getLoggerName());
            log.put("message", event.getFormattedMessage());
            log.put("timestamp", event.getTimeStamp());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " +
                    Base64.getEncoder().encodeToString("elastic:changeme".getBytes()));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(log, headers);
            restTemplate.postForEntity(url, request, String.class);

        } catch (Exception e) {
            logger.error("Failed to send log to Elasticsearch:  {}", e.getMessage(), e);
        }
    }
}

