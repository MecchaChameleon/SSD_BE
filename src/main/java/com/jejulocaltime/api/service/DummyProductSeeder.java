package com.jejulocaltime.api.service;

import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.domain.ProductImage;
import com.jejulocaltime.api.domain.SellerApplication;
import com.jejulocaltime.api.domain.SellerProfile;
import com.jejulocaltime.api.domain.User;
import com.jejulocaltime.api.repository.ProductImageRepository;
import com.jejulocaltime.api.repository.ProductRepository;
import com.jejulocaltime.api.repository.SellerApplicationRepository;
import com.jejulocaltime.api.repository.SellerProfileRepository;
import com.jejulocaltime.api.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 데모/QA용 더미 상품 20개를 항상 노출 상태로 유지한다.
 *
 * product.reservation_close_at이 지나면 ProductExpirationService가 자동으로 CLOSED 처리하기 때문에,
 * 더미 상품을 한 번만 만들어 두면 하루 지나 목록에서 사라진다. 그래서 매일 같은 20개 상품을 지웠다
 * 새로 만드는 대신, 이미 있으면 시간/재고/상태만 오늘 기준으로 되돌리는 방식을 쓴다
 * (실사용자가 이 더미 상품을 찜/예약했을 수 있어 삭제 후 재생성은 FK 오류 위험이 있음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dummy-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DummyProductSeeder {

    private static final Long DUMMY_KAKAO_ID = -1L;
    private static final String DUMMY_BUSINESS_NUMBER = "000-00-00000";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalTime SALES_START_TIME = LocalTime.of(0, 5);
    private static final LocalTime SALES_CLOSE_TIME = LocalTime.of(23, 55);

    private final UserRepository userRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @PostConstruct
    public void seedOnStartup() {
        refresh();
    }

    @Scheduled(cron = "${dummy-data.cron:0 5 0 * * *}", zone = "Asia/Seoul")
    public void refresh() {
        SellerProfile sellerProfile = ensureDummySellerProfile();
        var salesDate = OffsetDateTime.now(SEOUL).toLocalDate();
        OffsetDateTime availableStartAt = salesDate.atTime(SALES_START_TIME).atZone(SEOUL).toOffsetDateTime();
        OffsetDateTime reservationCloseAt = salesDate.atTime(SALES_CLOSE_TIME).atZone(SEOUL).toOffsetDateTime();

        for (DummySpec spec : SPECS) {
            Product product = productRepository
                    .findBySellerProfileIdAndName(sellerProfile.getId(), spec.name())
                    .orElseGet(() -> createProduct(sellerProfile, spec, availableStartAt, reservationCloseAt));

            product.setTotalQuantity(spec.totalQuantity());
            product.setRemainingQuantity(spec.totalQuantity());
            product.setCurrentPrice(spec.originalPrice());
            product.setAvailableStartAt(availableStartAt);
            product.setReservationCloseAt(reservationCloseAt);
            product.setStatus(Product.Status.ACTIVE);
            product.setAiAutoPricingEnabled(true);
            productRepository.save(product);

            ensureProductImage(product.getId(), spec.imageFileName());
        }

        log.info("Dummy product refresh done: {} products active until {}", SPECS.size(), reservationCloseAt);
    }

    private Product createProduct(SellerProfile sellerProfile, DummySpec spec,
            OffsetDateTime availableStartAt, OffsetDateTime reservationCloseAt) {
        Product product = new Product();
        product.setSellerProfileId(sellerProfile.getId());
        product.setName(spec.name());
        product.setDescription(spec.description());
        product.setBusinessType(spec.businessType());
        product.setCategory(spec.category());
        product.setResourceType(Product.ResourceType.fromCategory(spec.category()));
        product.setEnvironmentType(spec.environmentType());
        product.setTotalQuantity(spec.totalQuantity());
        product.setRemainingQuantity(spec.totalQuantity());
        product.setOriginalPrice(spec.originalPrice());
        product.setMinimumPrice(spec.minimumPrice());
        product.setCurrentPrice(spec.originalPrice());
        product.setAiAutoPricingEnabled(true);
        product.setAvailableStartAt(availableStartAt);
        product.setReservationCloseAt(reservationCloseAt);
        product.setAddress(spec.address());
        product.setLatitude(spec.latitude());
        product.setLongitude(spec.longitude());
        product.setFootTrafficLevel(spec.footTrafficLevel());
        product.setStatus(Product.Status.ACTIVE);
        return productRepository.save(product);
    }

    /**
     * src/main/resources/static/dummy-images/{fileName}에 넣어둔 사진을 사용한다.
     * 이 폴더는 빌드 시 앱(jar/Docker 이미지)에 같이 포함되므로, S3 같은 별도 스토리지 없이도
     * 재배포 후에 사진이 사라지지 않는다.
     */
    private void ensureProductImage(Long productId, String fileName) {
        String imageUrl = publicBaseUrl + "/dummy-images/" + fileName;

        List<ProductImage> existingImages = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        if (existingImages.isEmpty()) {
            productImageRepository.save(new ProductImage(productId, imageUrl, 0));
            return;
        }

        ProductImage image = existingImages.get(0);
        if (!imageUrl.equals(image.getImageUrl())) {
            productImageRepository.delete(image);
            productImageRepository.save(new ProductImage(productId, imageUrl, 0));
        }
    }

    private SellerProfile ensureDummySellerProfile() {
        User user = userRepository.findByKakaoId(DUMMY_KAKAO_ID)
                .orElseGet(() -> userRepository.save(
                        new User(DUMMY_KAKAO_ID, "dummy-seller@jejulocaltime.local", "제주로컬타임(더미)", null)));

        SellerApplication application = sellerApplicationRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    SellerApplication newApplication = new SellerApplication();
                    newApplication.setUserId(user.getId());
                    newApplication.setBusinessName("제주로컬타임 데모 매장");
                    newApplication.setBusinessNumber(DUMMY_BUSINESS_NUMBER);
                    newApplication.setRepresentativeName("데모");
                    newApplication.setBusinessDocumentUrl("https://picsum.photos/seed/jeju-dummy-doc/400/300");
                    newApplication.approve();
                    return sellerApplicationRepository.save(newApplication);
                });

        return sellerProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    SellerProfile newProfile = new SellerProfile();
                    newProfile.setUserId(user.getId());
                    newProfile.setSellerApplicationId(application.getId());
                    newProfile.setBusinessName(application.getBusinessName());
                    newProfile.setBusinessNumber(DUMMY_BUSINESS_NUMBER);
                    newProfile.setRepresentativeName(application.getRepresentativeName());
                    newProfile.setAddress("제주특별자치도 제주시 (데모)");
                    newProfile.setLatitude(new BigDecimal("33.4996000"));
                    newProfile.setLongitude(new BigDecimal("126.5312000"));
                    newProfile.setVerificationStatus(SellerProfile.VerificationStatus.VERIFIED);
                    return sellerProfileRepository.save(newProfile);
                });
    }

    private record DummySpec(
            String name,
            String description,
            Product.BusinessType businessType,
            Product.Category category,
            Product.EnvironmentType environmentType,
            int totalQuantity,
            int originalPrice,
            int minimumPrice,
            Product.FootTrafficLevel footTrafficLevel,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageFileName) {
    }

    private static final List<DummySpec> SPECS = List.of(
            new DummySpec("[데모] 제주 흑돼지 맛집 당일특가", "당일 마감 전 한정 수량 특가", Product.BusinessType.RESTAURANT,
                    Product.Category.SAME_DAY_INVENTORY, Product.EnvironmentType.INDOOR, 10, 25000, 15000,
                    Product.FootTrafficLevel.HIGH, "제주시 노형동", new BigDecimal("33.4890000"), new BigDecimal("126.4983000"),
                    "korean-bbq.jpg"),
            new DummySpec("[데모] 성산 해산물식당 저녁예약 특가", "저녁 예약 마감 임박 할인", Product.BusinessType.RESTAURANT,
                    Product.Category.SAME_DAY_INVENTORY, Product.EnvironmentType.INDOOR, 8, 30000, 18000,
                    Product.FootTrafficLevel.HIGH, "서귀포시 성산읍", new BigDecimal("33.4587000"), new BigDecimal("126.9280000"),
                    "seafood.jpg"),
            new DummySpec("[데모] 애월 카페 브런치세트 마감할인", "오후 브런치 세트 재고 소진 할인", Product.BusinessType.RESTAURANT,
                    Product.Category.EMPTY_TIME_RESOURCE, Product.EnvironmentType.INDOOR, 12, 18000, 10000,
                    Product.FootTrafficLevel.LOW, "제주시 애월읍", new BigDecimal("33.4630000"), new BigDecimal("126.3290000"),
                    "cafe-brunch.jpg"),
            new DummySpec("[데모] 조천 국수거리 점심 한정수량", "점심 한정 수량 소진 임박", Product.BusinessType.RESTAURANT,
                    Product.Category.SAME_DAY_INVENTORY, Product.EnvironmentType.INDOOR, 15, 9000, 6000,
                    Product.FootTrafficLevel.LOW, "제주시 조천읍", new BigDecimal("33.5390000"), new BigDecimal("126.6420000"),
                    "noodles.jpg"),
            new DummySpec("[데모] 표선 갈치조림 저녁 재고특가", "저녁 재고 소진 특가", Product.BusinessType.RESTAURANT,
                    Product.Category.SAME_DAY_INVENTORY, Product.EnvironmentType.INDOOR, 6, 28000, 17000,
                    Product.FootTrafficLevel.HIGH, "서귀포시 표선면", new BigDecimal("33.3260000"), new BigDecimal("126.8330000"),
                    "fish-stew.jpg"),

            new DummySpec("[데모] 제주시 게스트하우스 당일빈방", "당일 발생한 빈 객실 특가", Product.BusinessType.LODGING,
                    Product.Category.SAME_DAY_ROOM, Product.EnvironmentType.INDOOR, 3, 45000, 25000,
                    Product.FootTrafficLevel.LOW, "제주시 삼도동", new BigDecimal("33.5100000"), new BigDecimal("126.5220000"),
                    "guesthouse.jpg"),
            new DummySpec("[데모] 서귀포 오션뷰 펜션 당일특가", "당일 예약 가능 오션뷰 객실", Product.BusinessType.LODGING,
                    Product.Category.SAME_DAY_ROOM, Product.EnvironmentType.INDOOR, 2, 90000, 55000,
                    Product.FootTrafficLevel.HIGH, "서귀포시 중문동", new BigDecimal("33.2460000"), new BigDecimal("126.4120000"),
                    "ocean-view.jpg"),
            new DummySpec("[데모] 협재 감성숙소 남은객실 할인", "남은 객실 마감 할인", Product.BusinessType.LODGING,
                    Product.Category.SAME_DAY_ROOM, Product.EnvironmentType.INDOOR, 4, 70000, 40000,
                    Product.FootTrafficLevel.LOW, "제주시 한림읍 협재리", new BigDecimal("33.3940000"), new BigDecimal("126.2400000"),
                    "boutique-guesthouse.jpg"),
            new DummySpec("[데모] 중문 리조트 스탠다드룸 마감특가", "당일 마감 스탠다드룸 특가", Product.BusinessType.LODGING,
                    Product.Category.SAME_DAY_ROOM, Product.EnvironmentType.INDOOR, 2, 120000, 70000,
                    Product.FootTrafficLevel.HIGH, "서귀포시 중문관광로", new BigDecimal("33.2500000"), new BigDecimal("126.4150000"),
                    "resort.jpg"),
            new DummySpec("[데모] 한림 독채펜션 당일예약 할인", "당일 예약 시 즉시 할인", Product.BusinessType.LODGING,
                    Product.Category.SAME_DAY_ROOM, Product.EnvironmentType.INDOOR, 1, 85000, 50000,
                    Product.FootTrafficLevel.LOW, "제주시 한림읍", new BigDecimal("33.4110000"), new BigDecimal("126.2680000"),
                    "pension.jpg"),

            new DummySpec("[데모] 제주 승마체험 오후 빈자리", "오후 시간대 빈 자리 특가", Product.BusinessType.EXPERIENCE,
                    Product.Category.EMPTY_TIME_RESOURCE, Product.EnvironmentType.OUTDOOR, 5, 40000, 25000,
                    Product.FootTrafficLevel.LOW, "제주시 조천읍", new BigDecimal("33.4750000"), new BigDecimal("126.6700000"),
                    "horseback-riding.jpg"),
            new DummySpec("[데모] 우도 스노클링 체험 잔여석", "잔여 좌석 한정 특가", Product.BusinessType.EXPERIENCE,
                    Product.Category.EMPTY_TIME_RESOURCE, Product.EnvironmentType.OUTDOOR, 6, 55000, 35000,
                    Product.FootTrafficLevel.HIGH, "제주시 우도면", new BigDecimal("33.5010000"), new BigDecimal("126.9520000"),
                    "snorkeling.jpg"),
            new DummySpec("[데모] 감귤따기 체험 오후 타임", "오후 타임 한정 체험", Product.BusinessType.EXPERIENCE,
                    Product.Category.EMPTY_TIME_RESOURCE, Product.EnvironmentType.OUTDOOR, 10, 15000, 9000,
                    Product.FootTrafficLevel.LOW, "서귀포시 남원읍", new BigDecimal("33.2790000"), new BigDecimal("126.7180000"),
                    "tangerine.jpg"),
            new DummySpec("[데모] 요가 클래스 저녁 빈자리", "저녁 클래스 빈 자리 할인", Product.BusinessType.EXPERIENCE,
                    Product.Category.EMPTY_TIME_RESOURCE, Product.EnvironmentType.INDOOR, 8, 20000, 12000,
                    Product.FootTrafficLevel.LOW, "제주시 이도동", new BigDecimal("33.5000000"), new BigDecimal("126.5400000"),
                    "yoga.jpg"),
            new DummySpec("[데모] 도자기공방 원데이클래스 잔여", "원데이클래스 잔여 자리", Product.BusinessType.EXPERIENCE,
                    Product.Category.EMPTY_TIME_RESOURCE, Product.EnvironmentType.INDOOR, 4, 35000, 22000,
                    Product.FootTrafficLevel.LOW, "서귀포시 안덕면", new BigDecimal("33.2530000"), new BigDecimal("126.3320000"),
                    "pottery.jpg"),

            new DummySpec("[데모] 전기자전거 대여 당일 잔여분", "당일 렌탈 재고 특가", Product.BusinessType.RENTAL_MOBILITY,
                    Product.Category.TOUR_REMAINDER, Product.EnvironmentType.OUTDOOR, 12, 20000, 12000,
                    Product.FootTrafficLevel.HIGH, "제주시 연동", new BigDecimal("33.4870000"), new BigDecimal("126.4900000"),
                    "electric-bike.jpg"),
            new DummySpec("[데모] 제주 반나절 투어버스 잔여좌석", "반나절 투어 잔여 좌석 특가", Product.BusinessType.RENTAL_MOBILITY,
                    Product.Category.TOUR_REMAINDER, Product.EnvironmentType.OUTDOOR, 6, 45000, 28000,
                    Product.FootTrafficLevel.HIGH, "제주시 연동", new BigDecimal("33.4900000"), new BigDecimal("126.4930000"),
                    "tour-bus.jpg"),
            new DummySpec("[데모] 스쿠터 렌탈 당일 마감할인", "당일 마감 임박 렌탈 할인", Product.BusinessType.RENTAL_MOBILITY,
                    Product.Category.TOUR_REMAINDER, Product.EnvironmentType.OUTDOOR, 8, 25000, 15000,
                    Product.FootTrafficLevel.LOW, "서귀포시 표선면", new BigDecimal("33.3270000"), new BigDecimal("126.8350000"),
                    "scooter.jpg"),
            new DummySpec("[데모] 카약 체험 투어 잔여자리", "잔여 자리 한정 특가", Product.BusinessType.RENTAL_MOBILITY,
                    Product.Category.TOUR_REMAINDER, Product.EnvironmentType.OUTDOOR, 5, 38000, 24000,
                    Product.FootTrafficLevel.LOW, "서귀포시 성산읍", new BigDecimal("33.4600000"), new BigDecimal("126.9300000"),
                    "kayak.jpg"),
            new DummySpec("[데모] 제주 야간 드라이브 투어 잔여석", "야간 투어 잔여석 특가", Product.BusinessType.RENTAL_MOBILITY,
                    Product.Category.TOUR_REMAINDER, Product.EnvironmentType.OUTDOOR, 6, 42000, 26000,
                    Product.FootTrafficLevel.HIGH, "제주시 애월읍", new BigDecimal("33.4600000"), new BigDecimal("126.3100000"),
                    "night-drive.jpg")
    );
}
