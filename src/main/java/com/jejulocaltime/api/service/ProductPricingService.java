package com.jejulocaltime.api.service;

import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.dto.AiPriceRequest;
import com.jejulocaltime.api.dto.AiPriceResponse;
import com.jejulocaltime.api.dto.AiStrategyRequest;
import com.jejulocaltime.api.dto.AiStrategyResponse;
import com.jejulocaltime.api.dto.ProductPriceDto;
import com.jejulocaltime.api.dto.ProductStrategyDto;
import com.jejulocaltime.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPricingService {

    private static final long UPDATE_INTERVAL_MINUTES = 10;

    private final ProductAccessGuard accessGuard;
    private final AiPricingClient aiPricingClient;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductPriceDto.Response getPriceRecommendation(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        return toResponse(product, requestRecommendation(product));
    }

    @Transactional
    public ProductPriceDto.AutoPricingResponse setAutoPricing(Long userId, Long productId, boolean enabled) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        product.setAiAutoPricingEnabled(enabled);
        if (enabled) {
            applyRecommendation(product, requestRecommendation(product));
        }
        productRepository.save(product);
        return autoState(product);
    }

    public ProductPriceDto.AutoPricingResponse getAutoPricing(Long userId, Long productId) {
        return autoState(accessGuard.requireOwnedProduct(userId, productId));
    }

    @Transactional
    public void recalculateAndApply(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getStatus() != Product.Status.ACTIVE || !product.isAiAutoPricingEnabled()) {
            return;
        }
        applyRecommendation(product, requestRecommendation(product));
        productRepository.save(product);
    }

    public ProductStrategyDto.Response getStrategy(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        AiStrategyResponse response = aiPricingClient.getStrategyRecommendation(new AiStrategyRequest(
                product.getId(), enumName(product.getCategory()), value(product.getRemainingQuantity(), 0),
                String.valueOf(product.getReservationCloseAt()), enumName(product.getFootTrafficLevel())
        ));
        return new ProductStrategyDto.Response(response.message());
    }

    private AiPriceResponse requestRecommendation(Product product) {
        OffsetDateTime now = OffsetDateTime.now();
        int remainingQuantity = value(product.getRemainingQuantity(), 0);
        int inventoryChange = product.getAiLastObservedQuantity() == null
                ? 0
                : remainingQuantity - product.getAiLastObservedQuantity();
        AiPriceRequest request = new AiPriceRequest(
                product.getId(), product.getSellerProfileId(), enumName(product.getBusinessType()),
                enumName(product.getCategory()), product.getOriginalPrice(), product.getMinimumPrice(),
                product.getCurrentPrice(), value(product.getTotalQuantity(), 0), remainingQuantity, inventoryChange,
                stringValue(product.getAvailableStartAt()), stringValue(product.getReservationCloseAt()), now.toString(),
                product.getAddress(), decimal(product.getLatitude()), decimal(product.getLongitude()),
                enumName(product.getFootTrafficLevel())
        );
        return aiPricingClient.getPriceRecommendation(request);
    }

    private void applyRecommendation(Product product, AiPriceResponse response) {
        int minimum = value(product.getMinimumPrice(), 0);
        int maximum = Math.max(minimum, value(product.getOriginalPrice(), minimum));
        int recommended = Math.max(minimum, Math.min(maximum, value(response.currentPrice(), maximum)));
        Integer previous = product.getCurrentPrice();
        product.setCurrentPrice(recommended);
        product.setAiLastPricedAt(OffsetDateTime.now());
        product.setAiModelVersion(response.modelVersion());
        product.setAiLastObservedQuantity(value(product.getRemainingQuantity(), 0));
        if (previous == null || previous != recommended) {
            jdbcTemplate.update("""
                    INSERT INTO product_price_history
                        (product_id, previous_price, changed_price, change_type, changed_at)
                    VALUES (?, ?, ?, 'AI_RECOMMENDATION', now())
                    """, product.getId(), previous, recommended);
        }
    }

    private ProductPriceDto.Response toResponse(Product product, AiPriceResponse response) {
        List<ProductPriceDto.PricePoint> timeline = response.priceTimeline() == null ? List.of() :
                response.priceTimeline().stream().map(p -> new ProductPriceDto.PricePoint(p.time(), p.price())).toList();
        List<ProductPriceDto.Explanation> explanations = response.explanations() == null ? List.of() :
                response.explanations().stream().map(e -> new ProductPriceDto.Explanation(
                        e.feature(), e.label(), e.value(), e.displayValue(), e.impact(), e.direction())).toList();
        ProductPriceDto.AutoPricingResponse state = autoState(product);
        return new ProductPriceDto.Response(
                response.currentPrice(), response.discountPct(), response.minutesLeft(), timeline,
                response.confidence(), response.modelVersion(), response.reason(), response.explanationMethod(),
                explanations, response.weatherSummary(), state.enabled(), state.lastUpdatedAt(), state.nextUpdateAt()
        );
    }

    private ProductPriceDto.AutoPricingResponse autoState(Product product) {
        OffsetDateTime last = product.getAiLastPricedAt();
        return new ProductPriceDto.AutoPricingResponse(product.isAiAutoPricingEnabled(), stringValue(last),
                last == null || !product.isAiAutoPricingEnabled() ? null : last.plusMinutes(UPDATE_INTERVAL_MINUTES).toString());
    }

    private static int value(Integer value, int fallback) { return value == null ? fallback : value; }
    private static Double decimal(java.math.BigDecimal value) { return value == null ? null : value.doubleValue(); }
    private static String stringValue(Object value) { return value == null ? null : value.toString(); }
    private static String enumName(Enum<?> value) { return value == null ? null : value.name(); }
}
