package com.jejulocaltime.api.domain.product;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.domain.product.dto.ProductDto;
import com.jejulocaltime.api.domain.reservation.Reservation;
import com.jejulocaltime.api.domain.reservation.ReservationRepository;
import com.jejulocaltime.api.domain.seller.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final List<Reservation.Status> ACTIVE_RESERVATION_STATUSES =
            List.of(Reservation.Status.REQUESTED, Reservation.Status.APPROVED);

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final ProductAccessGuard accessGuard;

    // 1. 상품/자원 등록
    @Transactional
    public ProductDto.Response createProduct(Long userId, ProductDto.CreateRequest request) {
        SellerProfile profile = accessGuard.requireSellerProfile(userId);

        Product product = new Product();
        product.setSellerProfileId(profile.getId());
        product.setName(request.name());
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
        product.setLatitude(toBigDecimal(request.lat()));
        product.setLongitude(toBigDecimal(request.lng()));
        product.setStatus(Product.Status.ACTIVE);

        Product saved = productRepository.save(product);
        return ProductDto.Response.from(saved);
    }

    // 2. 상품/자원 수정 (부분 필드, 전부 nullable)
    @Transactional
    public ProductDto.Response updateProduct(Long userId, Long productId, ProductDto.UpdateRequest request) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);

        if (request.name() != null) product.setName(request.name());
        if (request.category() != null) {
            product.setCategory(request.category());
            product.setResourceType(Product.ResourceType.fromCategory(request.category()));
        }
        if (request.type() != null) product.setEnvironmentType(request.type());
        // qty는 remaining_quantity(판매 가능 수량)만 갱신한다. total_quantity(최초 등록 수량)는 유지.
        if (request.qty() != null) product.setRemainingQuantity(request.qty());
        if (request.price() != null) {
            product.setOriginalPrice(request.price());
            product.setCurrentPrice(request.price());
        }
        if (request.minPrice() != null) product.setMinimumPrice(request.minPrice());
        if (request.openTime() != null) product.setAvailableStartAt(request.openTime());
        if (request.deadline() != null) product.setReservationCloseAt(request.deadline());
        if (request.foot() != null) product.setFootTrafficLevel(request.foot());
        if (request.lat() != null) product.setLatitude(toBigDecimal(request.lat()));
        if (request.lng() != null) product.setLongitude(toBigDecimal(request.lng()));

        return ProductDto.Response.from(product);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    // 5. 내 상품/자원 목록 조회
    public Page<ProductDto.Response> getMyProducts(Long userId, Product.Status status, Pageable pageable) {
        SellerProfile profile = accessGuard.requireSellerProfile(userId);

        Page<Product> page = (status != null)
                ? productRepository.findBySellerProfileIdAndStatus(profile.getId(), status, pageable)
                : productRepository.findBySellerProfileId(profile.getId(), pageable);

        return page.map(ProductDto.Response::from);
    }

    // 6. 내 상품/자원 단건 조회
    public ProductDto.Response getMyProduct(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        return ProductDto.Response.from(product);
    }

    // 7. 상품/자원 삭제
    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);

        if (reservationRepository.existsByProductIdAndStatusIn(productId, ACTIVE_RESERVATION_STATUSES)) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_HAS_ACTIVE_RESERVATION,
                    "예약(REQUESTED/APPROVED)이 진행 중인 상품은 삭제할 수 없습니다.");
        }

        productRepository.delete(product);
    }

    // 9. 판매상태 변경
    @Transactional
    public ProductDto.Response updateStatus(Long userId, Long productId, Product.Status status) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);
        product.setStatus(status);
        return ProductDto.Response.from(product);
    }
}
