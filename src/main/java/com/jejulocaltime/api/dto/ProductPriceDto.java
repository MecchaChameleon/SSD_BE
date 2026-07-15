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
            List<PricePoint> priceTimeline,
            Double confidence,
            String modelVersion,
            String reason,
            String explanationMethod,
            List<Explanation> explanations,
            String weatherSummary,
            boolean autoPricingEnabled,
            String lastUpdatedAt,
            String nextUpdateAt
    ) {}

    public record PricePoint(
            String time,
            Integer price
    ) {}

    public record Explanation(
            String feature,
            String label,
            Double value,
            String displayValue,
            Double impact,
            String direction
    ) {}

    public record AutoPricingRequest(boolean enabled) {}

    public record AutoPricingResponse(boolean enabled, String lastUpdatedAt, String nextUpdateAt) {}
}
