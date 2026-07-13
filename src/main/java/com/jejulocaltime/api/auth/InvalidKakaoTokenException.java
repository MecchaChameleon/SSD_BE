package com.jejulocaltime.api.auth;

public class InvalidKakaoTokenException extends RuntimeException {

    public InvalidKakaoTokenException(String message) {
        super(message);
    }
}
