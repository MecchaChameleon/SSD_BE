package com.jejulocaltime.api.dto;

import com.jejulocaltime.api.common.util.NumberConversions;
import com.jejulocaltime.api.domain.Product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class ProductDto {

    // [POST] 상품/자원 등록
    @Schema(name="ProductCreateRequest") public record CreateRequest(
            @NotNull String name,
            @NotBlank @Size(max = 50) String description,
            @NotNull Product.BusinessType businessType,
            @NotNull Product.Category category,
            Product.EnvironmentType type,
            @NotNull Integer qty,
            @NotNull Integer price,
            @NotNull Integer minPrice,
            OffsetDateTime openTime,
            @NotNull OffsetDateTime deadline,
            Product.FootTrafficLevel foot,
            // address/lat/lng를 안 보내면 판매자 본인 SellerProfile에 등록된 매장 주소로 채워진다.
            String address,
            Double lat,
            Double lng
    ) {
        public CreateRequest(String name, Product.BusinessType businessType, Product.Category category, Product.EnvironmentType type, Integer qty, Integer price, Integer minPrice, OffsetDateTime openTime, OffsetDateTime deadline, Product.FootTrafficLevel foot, String address, Double lat, Double lng) {
            this(name, "테스트 상품 설명", businessType, category, type, qty, price, minPrice, openTime, deadline, foot, address, lat, lng);
        }
    }

    // [PUT] 상품/자원 수정 (전부 선택적 입력)
    @Schema(name="ProductUpdateRequest") public record UpdateRequest(
            String name,
            @Size(max = 50) String description,
            Product.BusinessType businessType,
            Product.Category category,
            Product.EnvironmentType type,
            Integer qty,
            Integer price,
            Integer minPrice,
            OffsetDateTime openTime,
            OffsetDateTime deadline,
            Product.FootTrafficLevel foot,
            String address,
            Double lat,
            Double lng,
            Product.Status status
    ) {
        public UpdateRequest(String name, Product.BusinessType businessType, Product.Category category, Product.EnvironmentType type, Integer qty, Integer price, Integer minPrice, OffsetDateTime openTime, OffsetDateTime deadline, Product.FootTrafficLevel foot, String address, Double lat, Double lng, Product.Status status) {
            this(name, null, businessType, category, type, qty, price, minPrice, openTime, deadline, foot, address, lat, lng, status);
        }
    }

    // [PATCH] 판매상태 변경
    public record StatusUpdateRequest(
            @NotNull Product.Status status
    ) {}

    // [공통] 상품/자원 응답
    @Schema(name="ProductResponse") public record Response(
            Long id,
            Long sellerProfileId,
            String name,
            String description,
            Product.BusinessType businessType,
            Product.Category category,
            Product.EnvironmentType type,
            Integer totalQty,
            Integer qty,
            Integer price,
            Integer minPrice,
            Integer currentPrice,
            OffsetDateTime openTime,
            OffsetDateTime deadline,
            Product.FootTrafficLevel foot,
            String address,
            Double lat,
            Double lng,
            List<String> imageUrls,
            Product.Status status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Response from(Product entity) { return from(entity, List.of()); }
        public static Response from(Product entity, List<String> imageUrls) {
            return new Response(
                    entity.getId(),
                    entity.getSellerProfileId(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.getBusinessType(),
                    entity.getCategory(),
                    entity.getEnvironmentType(),
                    entity.getTotalQuantity(),
                    entity.getRemainingQuantity(),
                    entity.getOriginalPrice(),
                    entity.getMinimumPrice(),
                    entity.getCurrentPrice(),
                    entity.getAvailableStartAt(),
                    entity.getReservationCloseAt(),
                    entity.getFootTrafficLevel(),
                    entity.getAddress(),
                    NumberConversions.toDouble(entity.getLatitude()),
                    NumberConversions.toDouble(entity.getLongitude()),
                    imageUrls,
                    entity.getStatus(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        }
    }
}
