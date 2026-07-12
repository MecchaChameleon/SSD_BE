package com.jejulocaltime.api.domain.product.ai;

import com.jejulocaltime.api.domain.product.ai.dto.AiPriceRequest;
import com.jejulocaltime.api.domain.product.ai.dto.AiPriceResponse;
import com.jejulocaltime.api.domain.product.ai.dto.AiStrategyRequest;
import com.jejulocaltime.api.domain.product.ai.dto.AiStrategyResponse;

import java.util.List;

/**
 * 실제 AI(FastAPI) 서버 없이 AiPricingClient에 의존하는 코드를 테스트하기 위한 가짜 구현체.
 * 목(mock) 서버를 띄우지 않아도 컴파일/테스트가 가능하도록 인터페이스로 분리해둔 덕분에 사용 가능하다.
 */
public class FakeAiPricingClient implements AiPricingClient {

    public static final AiPriceResponse DEFAULT_PRICE_RESPONSE = new AiPriceResponse(
            8000,
            0.2,
            30,
            List.of(new AiPriceResponse.PricePoint("18:00", 10000), new AiPriceResponse.PricePoint("18:30", 8000))
    );

    public static final AiStrategyResponse DEFAULT_STRATEGY_RESPONSE =
            new AiStrategyResponse("마감 30분 전입니다. 지금 20% 할인하면 완판 확률이 높아요.");

    private AiPriceResponse priceResponse = DEFAULT_PRICE_RESPONSE;
    private AiStrategyResponse strategyResponse = DEFAULT_STRATEGY_RESPONSE;
    private RuntimeException failure;

    public void setPriceResponse(AiPriceResponse priceResponse) {
        this.priceResponse = priceResponse;
    }

    public void setStrategyResponse(AiStrategyResponse strategyResponse) {
        this.strategyResponse = strategyResponse;
    }

    public void failNextCallWith(RuntimeException failure) {
        this.failure = failure;
    }

    @Override
    public AiPriceResponse getPriceRecommendation(AiPriceRequest request) {
        if (failure != null) {
            throw failure;
        }
        return priceResponse;
    }

    @Override
    public AiStrategyResponse getStrategyRecommendation(AiStrategyRequest request) {
        if (failure != null) {
            throw failure;
        }
        return strategyResponse;
    }
}
