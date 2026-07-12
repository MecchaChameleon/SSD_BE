package com.jejulocaltime.api.common.exception;

/**
 * 비즈니스 규칙 위반 시 던지는 공통 예외.
 * 서비스 레이어에서는 IllegalArgumentException/IllegalStateException 대신 이 예외 + ErrorCode를 사용한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
