package com.jejulocaltime.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 예외에서 사용하는 에러 코드 모음.
 * 판매자-상품관리(SEL-01~04) 기능 구현 과정에서 필요한 코드부터 우선 추가했고,
 * 다른 도메인 작업 시 이 enum에 이어서 추가하면 된다.
 */
public enum ErrorCode {

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    SELLER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "판매자 프로필을 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_HAS_ACTIVE_RESERVATION(HttpStatus.CONFLICT, "예약이 진행 중인 상품은 삭제할 수 없습니다."),

    // TODO: AI 파트와 실제 스펙(에러 응답 형식 포함) 확정 후 세분화 필요
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "AI 추천 서버 호출에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
