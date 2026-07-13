package com.jejulocaltime.api.dto;

/**
 * AI 판매 전략 제안 서버(FastAPI) 요청 바디.
 * TODO: AI 파트와 실제 스펙 확정 후 수정 필요.
 */
public record AiStrategyRequest(
        Long productId,
        String category,
        Integer remainingQty,
        String deadline,
        String footTrafficLevel
) {}
