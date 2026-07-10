package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.auth.kakao.InvalidKakaoTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidKakaoTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidKakaoToken(InvalidKakaoTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
    }
}
