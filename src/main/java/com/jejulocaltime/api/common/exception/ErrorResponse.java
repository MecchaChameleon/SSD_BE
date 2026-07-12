package com.jejulocaltime.api.common.exception;

/**
 * BusinessException 발생 시 GlobalExceptionHandler가 내려주는 공통 에러 응답 바디.
 */
public record ErrorResponse(boolean success, String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.name(), message);
    }
}
