package com.jejulocaltime.api.dto;

/**
 * AI 추천 할인가 서버(FastAPI) 요청 바디.
 * TODO: AI 파트와 실제 스펙(필드명/타입) 확정 후 수정 필요. 현재는 우리 쪽에서 필요할 것으로
 * 예상되는 최소 정보(상품 식별자 + 가격/재고/마감 컨텍스트)만 우선 정의했다.
 */
public record AiPriceRequest(
        Long productId,
        Integer originalPrice,
        Integer minimumPrice,
        Integer remainingQty,
        String deadline,
        String footTrafficLevel
) {}
