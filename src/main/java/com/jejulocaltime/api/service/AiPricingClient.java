package com.jejulocaltime.api.service;

import com.jejulocaltime.api.dto.AiPriceRequest;
import com.jejulocaltime.api.dto.AiPriceResponse;
import com.jejulocaltime.api.dto.AiStrategyRequest;
import com.jejulocaltime.api.dto.AiStrategyResponse;


/**
 * AI 파트가 FastAPI로 배포하는 추천/전략 서버 호출을 추상화한 인터페이스.
 *
 * 백엔드는 할인가/전략 문구를 직접 계산하지 않고 이 인터페이스를 통해 AI 서버가 내려준 값을
 * 그대로 응답으로 전달한다(SEL-03, SEL-04). 실제 구현체는 {@link AiPricingRestClient}이고,
 * 테스트에서는 목 서버 없이 검증할 수 있도록 인터페이스로 분리했다.
 *
 * TODO: AI 파트와 실제 API 경로/요청·응답 스펙 확정 후 이 인터페이스와 구현체를 함께 수정할 것.
 */
public interface AiPricingClient {

    AiPriceResponse getPriceRecommendation(AiPriceRequest request);

    AiStrategyResponse getStrategyRecommendation(AiStrategyRequest request);
}
