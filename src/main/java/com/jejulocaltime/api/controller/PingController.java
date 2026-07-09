package com.jejulocaltime.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "jeju-localtime-api",
                "status", "ok",
                "time", Instant.now().toString()
        );
    }
}
