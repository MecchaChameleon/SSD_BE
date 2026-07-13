package com.jejulocaltime.api.service;

import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.dto.AiPriceRequest;
import com.jejulocaltime.api.dto.AiPriceResponse;
import com.jejulocaltime.api.dto.AiStrategyRequest;
import com.jejulocaltime.api.dto.AiStrategyResponse;
import com.jejulocaltime.api.dto.ProductPriceDto;
import com.jejulocaltime.api.dto.ProductStrategyDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SEL-03(AI 추천 할인가 조회), SEL-04(판매 전략 제안 조회).
 * 백엔드는 값/문구를 직접 계산하지 않고, AiPricingClient를 통해 AI 서버(FastAPI) 응답을 그대로 전달한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPricingService {

    private final ProductAccessGuard accessGuard;
    private final AiPricingClient aiPricingClient;

    // 3. AI 추천 할인가 조회
    public ProductPriceDto.Response getPriceRecommendation(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);

        AiPriceRequest aiRequest = new AiPriceRequest(
                product.getId(),
                product.getOriginalPrice(),
                product.getMinimumPrice(),
                product.getRemainingQuantity(),
                String.valueOf(product.getReservationCloseAt()),
                product.getFootTrafficLevel() != null ? product.getFootTrafficLevel().name() : null
        );

        AiPriceResponse aiResponse = aiPricingClient.getPriceRecommendation(aiRequest);

        List<ProductPriceDto.PricePoint> timeline = aiResponse.priceTimeline() == null
                ? List.of()
                : aiResponse.priceTimeline().stream()
                        .map(point -> new ProductPriceDto.PricePoint(point.time(), point.price()))
                        .toList();

        return new ProductPriceDto.Response(
                aiResponse.currentPrice(),
                aiResponse.discountPct(),
                aiResponse.minutesLeft(),
                timeline
        );
    }

    // 4. 판매 전략 제안 조회
    public ProductStrategyDto.Response getStrategy(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);

        AiStrategyRequest aiRequest = new AiStrategyRequest(
                product.getId(),
                product.getCategory() != null ? product.getCategory().name() : null,
                product.getRemainingQuantity(),
                String.valueOf(product.getReservationCloseAt()),
                product.getFootTrafficLevel() != null ? product.getFootTrafficLevel().name() : null
        );

        AiStrategyResponse aiResponse = aiPricingClient.getStrategyRecommendation(aiRequest);
        return new ProductStrategyDto.Response(aiResponse.message());
    }
}
