package com.jejulocaltime.api.domain.product.dto;

import com.jejulocaltime.api.common.util.NumberConversions;
import com.jejulocaltime.api.domain.product.Product;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class ProductDto {

    // [POST] 상품/자원 등록
    @Schema(name="ProductCreateRequest") public record CreateRequest(
            @NotNull String name,
            @NotNull Product.BusinessType businessType,
            @NotNull Product.Category category,
            Product.EnvironmentType type,
            @NotNull Integer qty,
            @NotNull Integer price,
            @NotNull Integer minPrice,
            LocalDateTime openTime,
            @NotNull LocalDateTime deadline,
            Product.FootTrafficLevel foot,
            // address/lat/lng를 안 보내면 판매자 본인 SellerProfile에 등록된 매장 주소로 채워진다.
            String address,
            Double lat,
            Double lng
    ) {}

    // [PUT] 상품/자원 수정 (전부 선택적 입력)
    @Schema(name="ProductUpdateRequest") public record UpdateRequest(
            String name,
            Product.BusinessType businessType,
            Product.Category category,
            Product.EnvironmentType type,
            Integer qty,
            Integer price,
            Integer minPrice,
            LocalDateTime openTime,
            LocalDateTime deadline,
            Product.FootTrafficLevel foot,
            String address,
            Double lat,
            Double lng
    ) {}

    // [PATCH] 판매상태 변경
    public record StatusUpdateRequest(
            @NotNull Product.Status status
    ) {}

    // [공통] 상품/자원 응답
    @Schema(name="ProductResponse") public record Response(
            Long id,
            Long sellerProfileId,
            String name,
            Product.BusinessType businessType,
            Product.Category category,
            Product.EnvironmentType type,
            Integer totalQty,
            Integer qty,
            Integer price,
            Integer minPrice,
            Integer currentPrice,
            LocalDateTime openTime,
            LocalDateTime deadline,
            Product.FootTrafficLevel foot,
            String address,
            Double lat,
            Double lng,
            Product.Status status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Response from(Product entity) {
            return new Response(
                    entity.getId(),
                    entity.getSellerProfileId(),
                    entity.getName(),
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
                    entity.getStatus(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        }
    }
}
