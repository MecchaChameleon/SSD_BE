package com.jejulocaltime.api.domain.seller.dto;

import com.jejulocaltime.api.domain.seller.SellerApplication;
import java.time.LocalDateTime;

public class SellerApplicationDto {

    // [POST] 입점 신청 요청
    public record CreateRequest(
            String businessName,
            String businessNumber,
            String representativeName,
            String businessDocumentUrl
    ) {}

    // [공통] 입점 신청 응답
    public record Response(
            Long id,
            Long userId,
            String businessName,
            String businessNumber,
            String representativeName,
            String businessDocumentUrl,
            SellerApplication.ApplicationStatus status,
            String rejectionReason,
            LocalDateTime appliedAt,
            LocalDateTime reviewedAt
    ) {
        public static Response from(SellerApplication entity) {
            return new Response(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getBusinessName(),
                    entity.getBusinessNumber(),
                    entity.getRepresentativeName(),
                    entity.getBusinessDocumentUrl(),
                    entity.getStatus(),
                    entity.getRejectionReason(),
                    entity.getAppliedAt(),
                    entity.getReviewedAt()
            );
        }
    }
}