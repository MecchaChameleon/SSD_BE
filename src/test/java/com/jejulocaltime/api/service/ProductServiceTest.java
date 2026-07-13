package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.domain.Reservation;
import com.jejulocaltime.api.domain.SellerProfile;
import com.jejulocaltime.api.dto.ProductDto;
import com.jejulocaltime.api.repository.ProductRepository;
import com.jejulocaltime.api.repository.ReservationRepository;
import com.jejulocaltime.api.repository.SellerProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SELLER_PROFILE_ID = 100L;
    private static final Long PRODUCT_ID = 50L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        ProductAccessGuard accessGuard = new ProductAccessGuard(sellerProfileRepository, productRepository);
        productService = new ProductService(productRepository, reservationRepository, accessGuard);
    }

    @Test
    void 상품_등록시_요청한_판매자_프로필로_저장되고_qty가_total_remaining에_모두_반영된다() {
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto.CreateRequest request = new ProductDto.CreateRequest(
                "흑돼지 당일 특가", Product.BusinessType.RESTAURANT, Product.Category.SAME_DAY_INVENTORY,
                Product.EnvironmentType.INDOOR,
                10, 20000, 12000, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                Product.FootTrafficLevel.HIGH, "제주시 어딘가", 33.45, 126.56
        );

        ProductDto.Response response = productService.createProduct(USER_ID, request);

        assertThat(response.totalQty()).isEqualTo(10);
        assertThat(response.qty()).isEqualTo(10);
        assertThat(response.price()).isEqualTo(20000);
        assertThat(response.currentPrice()).isEqualTo(20000);
        assertThat(response.status()).isEqualTo(Product.Status.ACTIVE);
    }

    @Test
    void 업종에서_허용하지_않는_유형이면_등록이_거부된다() {
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));

        // LODGING(숙박)은 SAME_DAY_INVENTORY(당일재고)를 허용하지 않는다.
        ProductDto.CreateRequest request = new ProductDto.CreateRequest(
                "잘못된 조합", Product.BusinessType.LODGING, Product.Category.SAME_DAY_INVENTORY,
                Product.EnvironmentType.INDOOR,
                10, 20000, 12000, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                Product.FootTrafficLevel.HIGH, null, null, null
        );

        assertThatThrownBy(() -> productService.createProduct(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 매장_위치를_안_보내면_판매자_프로필_주소로_채워진다() {
        SellerProfile profile = sellerProfile();
        profile.setAddress("제주시 노형동 123");
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto.CreateRequest request = new ProductDto.CreateRequest(
                "흑돼지 당일 특가", Product.BusinessType.RESTAURANT, Product.Category.SAME_DAY_INVENTORY,
                Product.EnvironmentType.INDOOR,
                10, 20000, 12000, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                Product.FootTrafficLevel.HIGH, null, null, null
        );

        ProductDto.Response response = productService.createProduct(USER_ID, request);

        assertThat(response.address()).isEqualTo("제주시 노형동 123");
    }

    @Test
    void 등록시_최소금액이_최고금액보다_높으면_거부된다() {
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));

        // price(최고금액)=10000 < minPrice(최소금액)=20000 -> 잘못된 가격 조합
        ProductDto.CreateRequest request = new ProductDto.CreateRequest(
                "가격 뒤집힘", Product.BusinessType.RESTAURANT, Product.Category.SAME_DAY_INVENTORY,
                Product.EnvironmentType.INDOOR,
                10, 10000, 20000, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                Product.FootTrafficLevel.HIGH, null, null, null
        );

        assertThatThrownBy(() -> productService.createProduct(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 등록시_판매시작시간이_종료시간보다_뒤면_거부된다() {
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));

        // openTime(시작) = now+3h, deadline(종료) = now+1h -> 시작이 종료보다 뒤
        ProductDto.CreateRequest request = new ProductDto.CreateRequest(
                "시간 뒤집힘", Product.BusinessType.RESTAURANT, Product.Category.SAME_DAY_INVENTORY,
                Product.EnvironmentType.INDOOR,
                10, 20000, 12000, LocalDateTime.now().plusHours(3), LocalDateTime.now().plusHours(1),
                Product.FootTrafficLevel.HIGH, null, null, null
        );

        assertThatThrownBy(() -> productService.createProduct(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 수정시_판매시작시간이_종료시간보다_뒤면_거부된다() {
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(ownedProduct()));

        // 시작 now+5h / 종료 now+1h 로 함께 수정 -> 뒤집힌 기간이라 거부
        ProductDto.UpdateRequest request = new ProductDto.UpdateRequest(
                null, null, null, null, null, null, null,
                LocalDateTime.now().plusHours(5), LocalDateTime.now().plusHours(1), null, null, null, null, null);

        assertThatThrownBy(() -> productService.updateProduct(USER_ID, PRODUCT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 수정시_최소금액이_최고금액보다_높으면_거부된다() {
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(ownedProduct()));

        // 최고금액 10000 / 최소금액 20000 으로 함께 수정 -> 뒤집힌 조합이라 거부
        ProductDto.UpdateRequest request = new ProductDto.UpdateRequest(
                null, null, null, null, null, 10000, 20000, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> productService.updateProduct(USER_ID, PRODUCT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 상품_수정시_qty를_보내면_remaining_quantity만_바뀌고_total_quantity는_유지된다() {
        Product product = ownedProduct();
        product.setTotalQuantity(10);
        product.setRemainingQuantity(10);

        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductDto.UpdateRequest request = new ProductDto.UpdateRequest(
                null, null, null, null, 3, null, null, null, null, null, null, null, null, null);

        ProductDto.Response response = productService.updateProduct(USER_ID, PRODUCT_ID, request);

        assertThat(response.totalQty()).isEqualTo(10);
        assertThat(response.qty()).isEqualTo(3);
    }

    @Test
    void 진행중인_예약이_있으면_삭제가_거부된다() {
        Product product = ownedProduct();
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(reservationRepository.existsByProductIdAndStatusIn(
                eq(PRODUCT_ID), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> productService.deleteProduct(USER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_HAS_ACTIVE_RESERVATION);
    }

    @Test
    void 진행중인_예약이_없으면_정상적으로_삭제된다() {
        Product product = ownedProduct();
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(reservationRepository.existsByProductIdAndStatusIn(
                PRODUCT_ID,
                List.of(Reservation.Status.REQUESTED, Reservation.Status.APPROVED))).thenReturn(false);

        productService.deleteProduct(USER_ID, PRODUCT_ID);

        org.mockito.Mockito.verify(productRepository).delete(product);
    }

    @Test
    void 판매상태를_변경할_수_있다() {
        Product product = ownedProduct();
        when(sellerProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sellerProfile()));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductDto.Response response = productService.updateStatus(USER_ID, PRODUCT_ID, Product.Status.PAUSED);

        assertThat(response.status()).isEqualTo(Product.Status.PAUSED);
    }

    private SellerProfile sellerProfile() {
        SellerProfile profile = new SellerProfile();
        profile.setId(SELLER_PROFILE_ID);
        profile.setUserId(USER_ID);
        return profile;
    }

    private Product ownedProduct() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSellerProfileId(SELLER_PROFILE_ID);
        product.setStatus(Product.Status.ACTIVE);
        return product;
    }
}
