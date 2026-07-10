package com.jejulocaltime.api.auth.kakao;

public class InvalidKakaoTokenException extends RuntimeException {

    public InvalidKakaoTokenException(String message) {
        super(message);
    }
}
