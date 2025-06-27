package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BertMatcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(BertMatcherApplication.class, args);
    }
}
