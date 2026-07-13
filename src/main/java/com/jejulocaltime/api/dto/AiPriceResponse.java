package com.jejulocaltime.api.dto;

import java.util.List;

/**
 * AI 추천 할인가 서버(FastAPI) 응답 바디.
 * TODO: AI 파트와 실제 스펙(필드명/타입) 확정 후 수정 필요. 우리 쪽 응답(ProductPriceDto.Response)에
 * 필요한 형태(currentPrice, discountPct, minutesLeft, priceTimeline)를 기준으로 우선 정의했다.
 */
public record AiPriceResponse(
        Integer currentPrice,
        Double discountPct,
        Integer minutesLeft,
        List<PricePoint> priceTimeline
) {
    public record PricePoint(String time, Integer price) {}
}
