package com.jejulocaltime.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JejuLocaltimeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JejuLocaltimeApiApplication.class, args);
    }
}
