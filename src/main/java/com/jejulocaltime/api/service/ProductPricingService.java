package com.jejulocaltime.api.service;

import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.domain.SellerProfile;
import com.jejulocaltime.api.dto.AiPriceRequest;
import com.jejulocaltime.api.dto.AiPriceResponse;
import com.jejulocaltime.api.dto.AiStrategyRequest;
import com.jejulocaltime.api.dto.AiStrategyResponse;
import com.jejulocaltime.api.dto.ProductPriceDto;
import com.jejulocaltime.api.dto.ProductStrategyDto;
import com.jejulocaltime.api.repository.ProductRepository;
import com.jejulocaltime.api.repository.SellerProfileRepository;
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
    private final SellerProfileRepository sellerProfileRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ProductPriceDto.Response getPriceRecommendation(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        AiPriceResponse response = requestRecommendation(product);
        // 자동 가격 상품은 추천가 변경을 조회한 즉시 실제 판매가에도 반영한다.
        // 10분 스케줄러는 화면이 열려 있지 않을 때를 위한 보조 갱신으로 유지한다.
        if (product.getStatus() == Product.Status.ACTIVE && product.isAiAutoPricingEnabled()) {
            applyRecommendation(product, response);
            productRepository.save(product);
        }
        return toResponse(product, response);
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
        Long soldQuantityValue = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM reservation
                WHERE product_id = ?
                  AND payment_status = 'PAID'
                  AND status IN ('REQUESTED', 'COMPLETED')
                """, Long.class, product.getId());
        int soldQuantity = soldQuantityValue == null ? 0 : Math.toIntExact(soldQuantityValue);
        int pricingTotalQuantity = remainingQuantity + soldQuantity;
        int inventoryChange = product.getAiLastObservedQuantity() == null
                ? 0
                : remainingQuantity - product.getAiLastObservedQuantity();
        SellerProfile profile = sellerProfileRepository.findById(product.getSellerProfileId()).orElse(null);
        String address = locationAddress(product.getAddress(), profile == null ? null : profile.getAddress());
        java.math.BigDecimal latitude = product.getLatitude() != null || profile == null
                ? product.getLatitude() : profile.getLatitude();
        java.math.BigDecimal longitude = product.getLongitude() != null || profile == null
                ? product.getLongitude() : profile.getLongitude();
        AiPriceRequest request = new AiPriceRequest(
                product.getId(), product.getSellerProfileId(), enumName(product.getBusinessType()),
                enumName(product.getCategory()), product.getOriginalPrice(), product.getMinimumPrice(),
                product.getCurrentPrice(), pricingTotalQuantity, remainingQuantity, inventoryChange,
                stringValue(product.getAvailableStartAt()), stringValue(product.getReservationCloseAt()), now.toString(),
                address, decimal(latitude), decimal(longitude),
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
        ProductPriceDto.Weather weather = response.weather() == null ? null : new ProductPriceDto.Weather(
                response.weather().currentTemperature(), response.weather().currentPrecipitation(),
                response.weather().currentWindSpeed(), response.weather().forecastTemperature(),
                response.weather().forecastPrecipitation(), response.weather().forecastWindSpeed(),
                response.weather().source(), response.weather().observedAt()
        );
        ProductPriceDto.RegionalDemand regionalDemand = response.regionalDemand() == null ? null :
                new ProductPriceDto.RegionalDemand(
                        response.regionalDemand().region(), response.regionalDemand().percentile(),
                        response.regionalDemand().predictedVisitPopulation(), response.regionalDemand().source(),
                        response.regionalDemand().basisDate(), response.regionalDemand().trainingStartDate(),
                        response.regionalDemand().trainingEndDate()
                );
        return new ProductPriceDto.Response(
                response.currentPrice(), response.discountPct(), response.minutesLeft(), timeline,
                response.confidence(), response.modelVersion(), response.reason(), response.explanationMethod(),
                explanations, response.weatherSummary(), weather, regionalDemand,
                state.enabled(), state.lastUpdatedAt(), state.nextUpdateAt()
        );
    }

    private ProductPriceDto.AutoPricingResponse autoState(Product product) {
        OffsetDateTime last = product.getAiLastPricedAt();
        return new ProductPriceDto.AutoPricingResponse(product.isAiAutoPricingEnabled(), stringValue(last),
                last == null || !product.isAiAutoPricingEnabled() ? null : last.plusMinutes(UPDATE_INTERVAL_MINUTES).toString());
    }

    private static int value(Integer value, int fallback) { return value == null ? fallback : value; }
    private static Double decimal(java.math.BigDecimal value) { return value == null ? null : value.doubleValue(); }
    private static String locationAddress(String productAddress, String profileAddress) {
        if (productAddress == null || productAddress.isBlank()) return profileAddress;
        if (profileAddress == null || profileAddress.isBlank() || productAddress.contains(profileAddress)) return productAddress;
        return productAddress + " " + profileAddress;
    }
    private static String stringValue(Object value) { return value == null ? null : value.toString(); }
    private static String enumName(Enum<?> value) { return value == null ? null : value.name(); }
}
