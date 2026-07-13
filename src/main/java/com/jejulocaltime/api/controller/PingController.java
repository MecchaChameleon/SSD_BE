package com.jejulocaltime.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "System", description = "서비스 상태 확인 API")
public class PingController {

    @GetMapping("/")
    @Operation(summary = "서비스 상태 확인", description = "서비스 이름, 현재 상태, 서버 시각을 반환합니다. 인증 없이 호출할 수 있습니다.")
    public Map<String, Object> root() {
        return Map.of(
                "service", "jeju-localtime-api",
                "status", "ok",
                "time", Instant.now().toString()
        );
    }
}
