package com.jejulocaltime.api.domain.product;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.domain.product.ai.FakeAiPricingClient;
import com.jejulocaltime.api.domain.product.dto.ProductPriceDto;
import com.jejulocaltime.api.domain.product.dto.ProductStrategyDto;
import com.jejulocaltime.api.domain.seller.SellerProfile;
import com.jejulocaltime.api.domain.seller.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPricingServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SELLER_PROFILE_ID = 100L;
    private static final Long PRODUCT_ID = 50L;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private ProductRepository productRepository;

    private FakeAiPricingClient aiPricingClient;
    private ProductPricingService productPricingService;

    @BeforeEach
    void setUp() {
        ProductAccessGuard accessGuard = new ProductAccessGuard(sellerProfileRepository, productRepository);
        aiPricingClient = new FakeAiPricingClient();
        productPricingService = new ProductPricingService(accessGuard, aiPricingClient);

        SellerProfile profile = new SellerProfile();
        profile.setId(SELLER_PROFILE_ID);
        profile.setUserId(USER_ID);
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSellerProfileId(SELLER_PROFILE_ID);
        product.setOriginalPrice(20000);
        product.setMinimumPrice(12000);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    @Test
    void AI_서버_추천값을_그대로_전달한다() {
        ProductPriceDto.Response response = productPricingService.getPriceRecommendation(USER_ID, PRODUCT_ID);

        assertThat(response.currentPrice()).isEqualTo(FakeAiPricingClient.DEFAULT_PRICE_RESPONSE.currentPrice());
        assertThat(response.discountPct()).isEqualTo(FakeAiPricingClient.DEFAULT_PRICE_RESPONSE.discountPct());
        assertThat(response.minutesLeft()).isEqualTo(FakeAiPricingClient.DEFAULT_PRICE_RESPONSE.minutesLeft());
        assertThat(response.priceTimeline()).hasSize(2);
        assertThat(response.priceTimeline().get(0).time()).isEqualTo("18:00");
    }

    @Test
    void AI_서버가_생성한_전략_문구를_그대로_전달한다() {
        ProductStrategyDto.Response response = productPricingService.getStrategy(USER_ID, PRODUCT_ID);

        assertThat(response.message()).isEqualTo(FakeAiPricingClient.DEFAULT_STRATEGY_RESPONSE.message());
    }

    @Test
    void AI_서버_호출_실패는_BusinessException으로_그대로_전파된다() {
        // 실제 타임아웃/5xx -> BusinessException(AI_SERVICE_ERROR) 변환은 AiPricingRestClient가 담당하고,
        // ProductPricingService는 클라이언트가 던진 예외를 그대로 전파하기만 하면 된다는 것을 검증한다.
        aiPricingClient.failNextCallWith(new BusinessException(ErrorCode.AI_SERVICE_ERROR));

        assertThatThrownBy(() -> productPricingService.getPriceRecommendation(USER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    void 본인_상품이_아니면_가격_추천도_403으로_거부된다() {
        Product othersProduct = new Product();
        othersProduct.setId(PRODUCT_ID);
        othersProduct.setSellerProfileId(SELLER_PROFILE_ID + 1);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(othersProduct));

        assertThatThrownBy(() -> productPricingService.getPriceRecommendation(USER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
