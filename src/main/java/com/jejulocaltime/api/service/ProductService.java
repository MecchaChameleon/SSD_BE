package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.common.util.NumberConversions;
import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.domain.PaymentOrder;
import com.jejulocaltime.api.domain.SellerProfile;
import com.jejulocaltime.api.dto.ProductDto;
import com.jejulocaltime.api.repository.ProductRepository;
import com.jejulocaltime.api.repository.PaymentOrderRepository;
import com.jejulocaltime.api.repository.ProductImageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final List<PaymentOrder.Status> PENDING_PAYMENT_STATUSES =
            List.of(PaymentOrder.Status.REQUESTED);

    private final ProductRepository productRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ProductAccessGuard accessGuard;
    private final ProductImageRepository productImageRepository;

    private ProductDto.Response response(Product product) {
        return ProductDto.Response.from(product, productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream().map(image -> image.getImageUrl()).toList());
    }

    // 1. 상품/자원 등록
    @Transactional
    public ProductDto.Response createProduct(Long userId, ProductDto.CreateRequest request) {
        SellerProfile profile = accessGuard.requireSellerProfile(userId);
        requireCompatible(request.businessType(), request.category());
        requireValidPriceRange(request.price(), request.minPrice());
        requireValidSalePeriod(request.openTime(), request.deadline());

        Product product = new Product();
        product.setSellerProfileId(profile.getId());
        product.setName(request.name());
        product.setBusinessType(request.businessType());
        product.setCategory(request.category());
        product.setResourceType(Product.ResourceType.fromCategory(request.category()));
        product.setEnvironmentType(request.type());
        product.setTotalQuantity(request.qty());
        product.setRemainingQuantity(request.qty());
        product.setOriginalPrice(request.price());
        product.setMinimumPrice(request.minPrice());
        product.setCurrentPrice(request.price());
        product.setAvailableStartAt(request.openTime());
        product.setReservationCloseAt(request.deadline());
        product.setFootTrafficLevel(request.foot());
        // 매장 위치(주소/좌표)를 안 보내면 판매자 본인 SellerProfile에 등록된 매장 정보로 채운다.
        product.setAddress(request.address() != null ? request.address() : profile.getAddress());
        product.setLatitude(request.lat() != null ? NumberConversions.toBigDecimal(request.lat()) : profile.getLatitude());
        product.setLongitude(request.lng() != null ? NumberConversions.toBigDecimal(request.lng()) : profile.getLongitude());
        product.setStatus(Product.Status.ACTIVE);

        Product saved = productRepository.save(product);
        return response(saved);
    }

    // 2. 상품/자원 수정 (부분 필드, 전부 nullable)
    @Transactional
    public ProductDto.Response updateProduct(Long userId, Long productId, ProductDto.UpdateRequest request) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);

        if (request.name() != null) product.setName(request.name());
        if (request.businessType() != null) product.setBusinessType(request.businessType());
        if (request.category() != null) {
            product.setCategory(request.category());
            product.setResourceType(Product.ResourceType.fromCategory(request.category()));
        }
        // 업종/유형 중 하나만 바뀌어도 조합이 깨질 수 있어서, 실제로 바뀐 게 있을 때만 최종 조합을 검증한다.
        if (request.businessType() != null || request.category() != null) {
            requireCompatible(product.getBusinessType(), product.getCategory());
        }
        if (request.type() != null) product.setEnvironmentType(request.type());
        // qty는 remaining_quantity(판매 가능 수량)만 갱신한다. total_quantity(최초 등록 수량)는 유지.
        if (request.qty() != null) {
            product.setRemainingQuantity(request.qty());
            if (request.qty() <= 0) product.setStatus(Product.Status.PAUSED);
        }
        if (request.price() != null) {
            product.setOriginalPrice(request.price());
            product.setCurrentPrice(request.price());
        }
        if (request.minPrice() != null) product.setMinimumPrice(request.minPrice());
        // 최고금액/최소금액 중 하나라도 바뀌면 최종 조합(최소금액 <= 최고금액)을 검증한다.
        if (request.price() != null || request.minPrice() != null) {
            requireValidPriceRange(product.getOriginalPrice(), product.getMinimumPrice());
        }
        if (request.openTime() != null) product.setAvailableStartAt(request.openTime());
        if (request.deadline() != null) product.setReservationCloseAt(request.deadline());
        // 판매시작/종료 중 하나라도 바뀌면 최종 판매기간(시작 < 종료)을 검증한다.
        if (request.openTime() != null || request.deadline() != null) {
            requireValidSalePeriod(product.getAvailableStartAt(), product.getReservationCloseAt());
        }
        if (request.foot() != null) product.setFootTrafficLevel(request.foot());
        if (request.address() != null) product.setAddress(request.address());
        if (request.lat() != null) product.setLatitude(NumberConversions.toBigDecimal(request.lat()));
        if (request.lng() != null) product.setLongitude(NumberConversions.toBigDecimal(request.lng()));
        if (request.status() != null) {
            if (request.status() == Product.Status.ACTIVE) {
                if (product.getRemainingQuantity() == null || product.getRemainingQuantity() <= 0) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "판매중으로 변경하려면 수량이 1개 이상이어야 합니다.");
                }
                if (product.getReservationCloseAt() == null || !product.getReservationCloseAt().isAfter(OffsetDateTime.now())) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "판매중으로 변경하려면 마감 시각이 현재보다 이후여야 합니다.");
                }
            }
            product.setStatus(request.status());
        }

        return response(product);
    }

    private void requireCompatible(Product.BusinessType businessType, Product.Category category) {
        if (!businessType.allows(category)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "선택한 업종(" + businessType + ")에서는 해당 유형(" + category + ")을 선택할 수 없습니다.");
        }
    }

    // 최소금액(minimumPrice)은 최고금액(originalPrice)보다 높을 수 없다.
    private void requireValidPriceRange(Integer originalPrice, Integer minPrice) {
        if (originalPrice != null && minPrice != null && minPrice > originalPrice) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "최소금액(" + minPrice + ")은 최고금액(" + originalPrice + ")보다 높을 수 없습니다.");
        }
    }

    // 판매 시작시간(availableStartAt)은 종료(마감)시간(reservationCloseAt)보다 앞서야 한다.
    private void requireValidSalePeriod(OffsetDateTime openTime, OffsetDateTime deadline) {
        if (openTime != null && deadline != null && !openTime.isBefore(deadline)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "판매 시작시간(" + openTime + ")은 종료시간(" + deadline + ")보다 앞서야 합니다.");
        }
    }

    // 5. 내 상품/자원 목록 조회
    public Page<ProductDto.Response> getMyProducts(Long userId, Product.Status status, Pageable pageable) {
        SellerProfile profile = accessGuard.requireSellerProfile(userId);

        Page<Product> page = (status != null)
                ? productRepository.findBySellerProfileIdAndStatus(profile.getId(), status, pageable)
                : productRepository.findBySellerProfileId(profile.getId(), pageable);

        return page.map(this::response);
    }

    // 6. 내 상품/자원 단건 조회
    public ProductDto.Response getMyProduct(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        return response(product);
    }

    // 7. 상품/자원 삭제
    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);

        if (paymentOrderRepository.existsByProductIdAndStatusIn(productId, PENDING_PAYMENT_STATUSES)) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_HAS_PENDING_PAYMENT,
                    "판매자 확인 대기 중인 결제가 있는 상품은 삭제할 수 없습니다.");
        }

        productRepository.delete(product);
    }

    // 9. 판매상태 변경
    @Transactional
    public ProductDto.Response updateStatus(Long userId, Long productId, Product.Status status) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        product.setStatus(status);
        return response(product);
    }

    // 10. 결제 시 잔여 수량(재고) 차감
    @Transactional
    public void decreaseRemainingQuantity(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (product.getRemainingQuantity() <= 0) {
            throw new IllegalStateException("잔여 수량이 없어 결제할 수 없습니다.");
        }

        // 잔여 수량 1 차감
        product.setRemainingQuantity(product.getRemainingQuantity() - 1);
    }
}
