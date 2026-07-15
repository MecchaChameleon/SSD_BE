package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.dto.AiPriceRequest;
import com.jejulocaltime.api.dto.AiPriceResponse;
import com.jejulocaltime.api.dto.AiStrategyRequest;
import com.jejulocaltime.api.dto.AiStrategyResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * AI 추천/전략 서버(FastAPI) 호출 구현체. KakaoAuthService와 동일하게 Spring RestClient를 사용한다.
 *
 * TODO: AI 파트와 실제 API 경로("/v1/pricing", "/v1/strategy"는 가정) 및 요청/응답 스펙 확정 후 수정 필요.
 */
@Component
public class AiPricingRestClient implements AiPricingClient {

    private final RestClient aiApiClient;

    public AiPricingRestClient(
            @Value("${ai.service.base-url}") String baseUrl,
            @Value("${ai.service.api-key:}") String apiKey) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-AI-Service-Key", apiKey);
        }
        this.aiApiClient = builder.build();
    }

    @Override
    public AiPriceResponse getPriceRecommendation(AiPriceRequest request) {
        try {
            // TODO: 실제 경로/응답 스펙 확정 필요
            return aiApiClient.post()
                    .uri("/v1/pricing/recommendation")
                    .body(request)
                    .retrieve()
                    .body(AiPriceResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 추천 할인가 서버 호출에 실패했습니다.");
        }
    }

    @Override
    public AiStrategyResponse getStrategyRecommendation(AiStrategyRequest request) {
        try {
            // TODO: 실제 경로/응답 스펙 확정 필요
            return aiApiClient.post()
                    .uri("/v1/pricing/strategy")
                    .body(request)
                    .retrieve()
                    .body(AiStrategyResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 판매 전략 서버 호출에 실패했습니다.");
        }
    }
}
