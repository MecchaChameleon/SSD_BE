package com.jejulocaltime.api.domain.seller.dto;

import com.jejulocaltime.api.domain.seller.SellerProfile;
import java.time.LocalDateTime;

public class SellerProfileDto {

    // [POST] 프로필 최초 등록 (가게 위치 및 정산 정보만 받음)
    public record CreateRequest(
            String address,
            Double latitude,
            Double longitude,
            String bankName,
            String accountNumber,
            String accountHolder
    ) {}

    // [PUT] 프로필 수정 (모두 선택적 입력 가능)
    public record UpdateRequest(
            String address,
            Double latitude,
            Double longitude,
            String bankName,
            String accountNumber,
            String accountHolder
    ) {}

    // [공통] 프로필 전체 응답
    public record Response(
            Long id,
            Long userId,
            Long sellerApplicationId,
            String businessName,
            String businessNumber,
            String representativeName,
            String address,
            Double latitude,
            Double longitude,
            String bankName,
            String accountNumber,
            String accountHolder,
            SellerProfile.VerificationStatus verificationStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Response from(SellerProfile entity) {
            return new Response(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getSellerApplicationId(),
                    entity.getBusinessName(),
                    entity.getBusinessNumber(),
                    entity.getRepresentativeName(),
                    entity.getAddress(),
                    entity.getLatitude() != null ? entity.getLatitude().doubleValue() : null,
                    entity.getLongitude() != null ? entity.getLongitude().doubleValue() : null,
                    entity.getBankName(),
                    entity.getAccountNumber(),
                    entity.getAccountHolder(),
                    entity.getVerificationStatus(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        }
    }
}