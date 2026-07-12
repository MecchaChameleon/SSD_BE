package com.jejulocaltime.api.domain.product;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
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
class ProductAccessGuardTest {

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private ProductRepository productRepository;

    private ProductAccessGuard accessGuard;

    @BeforeEach
    void setUp() {
        accessGuard = new ProductAccessGuard(sellerProfileRepository, productRepository);
    }

    @Test
    void 판매자_프로필이_없으면_SELLER_PROFILE_NOT_FOUND_예외() {
        when(sellerProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessGuard.requireSellerProfile(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SELLER_PROFILE_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_상품이면_PRODUCT_NOT_FOUND_예외() {
        SellerProfile profile = sellerProfile(1L, 100L);
        when(sellerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessGuard.requireOwnedProduct(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void 본인_상품이_아니면_ACCESS_DENIED_예외() {
        SellerProfile myProfile = sellerProfile(1L, 100L);
        Product othersProduct = product(50L, 200L); // 다른 판매자(sellerProfileId=200)의 상품

        when(sellerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(myProfile));
        when(productRepository.findById(50L)).thenReturn(Optional.of(othersProduct));

        assertThatThrownBy(() -> accessGuard.requireOwnedProduct(1L, 50L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void 본인_상품이면_정상적으로_반환() {
        SellerProfile myProfile = sellerProfile(1L, 100L);
        Product myProduct = product(50L, 100L);

        when(sellerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(myProfile));
        when(productRepository.findById(50L)).thenReturn(Optional.of(myProduct));

        Product result = accessGuard.requireOwnedProduct(1L, 50L);

        assertThat(result).isSameAs(myProduct);
    }

    private SellerProfile sellerProfile(Long userId, Long profileId) {
        SellerProfile profile = new SellerProfile();
        profile.setId(profileId);
        profile.setUserId(userId);
        return profile;
    }

    private Product product(Long productId, Long sellerProfileId) {
        Product product = new Product();
        product.setId(productId);
        product.setSellerProfileId(sellerProfileId);
        return product;
    }
}
