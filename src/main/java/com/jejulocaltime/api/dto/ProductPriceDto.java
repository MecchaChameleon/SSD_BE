package com.jejulocaltime.api.dto;

import java.util.List;

/**
 * SEL-03 AI 추천 할인가 조회 응답.
 * TODO: AI 파트와 실제 스펙 확정 후 필드/타입 수정 필요.
 */
public class ProductPriceDto {

    public record Response(
            Integer currentPrice,
            Double discountPct,
            Integer minutesLeft,
            List<PricePoint> priceTimeline
    ) {}

    public record PricePoint(
            String time,
            Integer price
    ) {}
}
