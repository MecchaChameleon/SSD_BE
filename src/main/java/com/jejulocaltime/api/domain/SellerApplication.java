package com.jejulocaltime.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "seller_application")
@Getter
@Setter
@NoArgsConstructor
public class SellerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // users 테이블의 id와 매핑 (한 유저당 하나의 신청서만 유효하도록 unique 처리 고려 가능)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessNumber;

    @Column(name = "open_date")
    private String openDate;

    @Column(nullable = false)
    private String representativeName;

    // 사업자등록증 사본 이미지 URL
    @Column(nullable = false)
    private String businessDocumentUrl; 

    // 신청 상태 (대기, 승인, 반려)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    // 반려되었을 경우의 사유
    private String rejectionReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime reviewedAt;

    @Column(name = "nts_verified_at")
    private OffsetDateTime ntsVerifiedAt;

    @Column(name = "nts_business_status")
    private String ntsBusinessStatus;

    public enum ApplicationStatus {
        PENDING, APPROVED, REJECTED
    }
    
    // 승인/반려 처리를 위한 편의 메서드
    public void approve() {
        this.status = ApplicationStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }
    
    public void reject(String reason) {
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = LocalDateTime.now();
    }
}