package com.jejulocaltime.api.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 공통 성공 응답 포맷.
 * 컨트롤러는 ResponseEntity를 직접 생성하지 않고 이 클래스의 success() 팩토리 메서드만 사용한다.
 * 실패 응답은 이 클래스가 아니라 BusinessException + ErrorCode + GlobalExceptionHandler 조합으로 처리한다.
 */
public class ApiResponseTemplate<T> {

    private final boolean success;
    private final T data;

    private ApiResponseTemplate(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> success(T data) {
        return ResponseEntity.ok(new ApiResponseTemplate<>(true, data));
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> success(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(new ApiResponseTemplate<>(true, data));
    }

    public static ResponseEntity<ApiResponseTemplate<Void>> success() {
        return ResponseEntity.ok(new ApiResponseTemplate<>(true, null));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }
}
