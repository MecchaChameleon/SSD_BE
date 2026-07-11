package com.jejulocaltime.api.domain.seller;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
public class SellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // 승인된 입점 신청서와 연결 (필수)
    @Column(name = "seller_application_id", nullable = false, unique = true)
    private Long sellerApplicationId;

    // 신청서에서 복사해 올 변하지 않는 정보들 (수정 불가)
    @Column(nullable = false, updatable = false)
    private String businessName;

    @Column(nullable = false, unique = true, updatable = false)
    private String businessNumber;

    @Column(nullable = false, updatable = false)
    private String representativeName;

    // 사장님이 직접 등록/수정할 가게 및 정산 정보
    private String address;
    
    @Column(precision = 10, scale = 7)
    private Double latitude;
    
    @Column(precision = 10, scale = 7)
    private Double longitude;

    private String bankName;
    private String accountNumber;
    private String accountHolder;

    // 계좌 1원 인증 등 추가 인증 상태 관리용
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum VerificationStatus {
        UNVERIFIED, VERIFIED
    }
}