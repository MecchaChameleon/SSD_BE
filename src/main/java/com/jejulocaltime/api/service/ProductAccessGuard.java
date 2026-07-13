package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.domain.SellerProfile;
import com.jejulocaltime.api.repository.ProductRepository;
import com.jejulocaltime.api.repository.SellerProfileRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 상품 소유권(본인이 등록한 상품인지) 검사를 한 곳에 모아둔 공통 컴포넌트.
 * ProductService/ProductImageService/ProductPricingService가 모두 이 컴포넌트를 통해
 * 소유권을 검사해서 동일한 로직이 여러 서비스에 중복되지 않도록 한다.
 */
@Component
@RequiredArgsConstructor
public class ProductAccessGuard {

    private final SellerProfileRepository sellerProfileRepository;
    private final ProductRepository productRepository;

    /** 로그인한 유저(userId)의 판매자 프로필을 조회한다. 없으면 프로필 미등록 오류. */
    public SellerProfile requireSellerProfile(Long userId) {
        return sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SELLER_PROFILE_NOT_FOUND, "판매자 프로필이 등록되어 있지 않습니다."));
    }

    /** productId가 존재하고, 로그인한 유저 본인이 등록한 상품인지 검사 후 반환한다. */
    public Product requireOwnedProduct(Long userId, Long productId) {
        SellerProfile profile = requireSellerProfile(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(profile.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인이 등록한 상품이 아닙니다.");
        }
        return product;
    }
}
