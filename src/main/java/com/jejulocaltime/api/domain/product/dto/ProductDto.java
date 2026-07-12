package com.jejulocaltime.api.domain.product.dto;

import com.jejulocaltime.api.domain.product.Product;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ProductDto {

    // [POST] 상품/자원 등록
    public record CreateRequest(
            @NotNull String name,
            @NotNull Product.Category category,
            Product.EnvironmentType type,
            @NotNull Integer qty,
            @NotNull Integer price,
            @NotNull Integer minPrice,
            LocalDateTime openTime,
            @NotNull LocalDateTime deadline,
            Product.FootTrafficLevel foot,
            Double lat,
            Double lng
    ) {}

    // [PUT] 상품/자원 수정 (전부 선택적 입력)
    public record UpdateRequest(
            String name,
            Product.Category category,
            Product.EnvironmentType type,
            Integer qty,
            Integer price,
            Integer minPrice,
            LocalDateTime openTime,
            LocalDateTime deadline,
            Product.FootTrafficLevel foot,
            Double lat,
            Double lng
    ) {}

    // [PATCH] 판매상태 변경
    public record StatusUpdateRequest(
            @NotNull Product.Status status
    ) {}

    // [공통] 상품/자원 응답
    public record Response(
            Long id,
            Long sellerProfileId,
            String name,
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
                    entity.getLatitude() != null ? entity.getLatitude().doubleValue() : null,
                    entity.getLongitude() != null ? entity.getLongitude().doubleValue() : null,
                    entity.getStatus(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        }
    }
}
