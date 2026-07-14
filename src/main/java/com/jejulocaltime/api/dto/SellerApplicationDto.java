package com.jejulocaltime.api.dto;

import com.jejulocaltime.api.domain.SellerApplication;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class SellerApplicationDto {

    // [POST] 입점 신청 요청
    @Schema(name = "SellerApplicationCreateRequest")
    public record CreateRequest(
            @NotBlank String businessName,
            @NotBlank String businessNumber,
            @NotBlank String representativeName,
            @NotBlank String openDate,
            String businessDocumentUrl
    ) {}

    // [공통] 입점 신청 응답
    @Schema(name = "SellerApplicationResponse")
    public record Response(
            Long id,
            Long userId,
            String businessName,
            String businessNumber,
            String openDate,
            String representativeName,
            String businessDocumentUrl,
            SellerApplication.ApplicationStatus status,
            String rejectionReason,
            OffsetDateTime ntsVerifiedAt,
            String ntsBusinessStatus,
            LocalDateTime appliedAt,
            LocalDateTime reviewedAt
    ) {
        public static Response from(SellerApplication entity) {
            return new Response(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getBusinessName(),
                    entity.getBusinessNumber(),
                    entity.getOpenDate(),
                    entity.getRepresentativeName(),
                    entity.getBusinessDocumentUrl(),
                    entity.getStatus(),
                    entity.getRejectionReason(),
                    entity.getNtsVerifiedAt(),
                    entity.getNtsBusinessStatus(),
                    entity.getAppliedAt(),
                    entity.getReviewedAt()
            );
        }
    }
}
