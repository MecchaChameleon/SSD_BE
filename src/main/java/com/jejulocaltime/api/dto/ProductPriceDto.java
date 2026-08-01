package com.jejulocaltime.api.dto;

import com.jejulocaltime.api.domain.Product;
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
            Weather weather,
            RegionalDemand regionalDemand,
            List<PriceOption> priceOptions,
            Product.PricingPurpose selectedPurpose,
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

    public record Weather(
            Double currentTemperature,
            Double currentPrecipitation,
            Double currentWindSpeed,
            Double forecastTemperature,
            Double forecastPrecipitation,
            Double forecastWindSpeed,
            String source,
            String observedAt
    ) {}

    public record RegionalDemand(
            String region,
            Double percentile,
            Integer predictedVisitPopulation,
            String source,
            String basisDate,
            String trainingStartDate,
            String trainingEndDate
    ) {}

    public record PriceOption(
            String purpose,
            String label,
            Integer price,
            Double discountPct,
            Integer salesLikelihoodIndex,
            Integer expectedRevenue,
            List<String> majorFactors
    ) {}

    public record AutoPricingRequest(boolean enabled, Product.PricingPurpose purpose) {}

    public record AutoPricingResponse(boolean enabled, Product.PricingPurpose purpose, String lastUpdatedAt, String nextUpdateAt) {}
}
