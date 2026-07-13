package com.jejulocaltime.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 예약 도메인의 최소 매핑.
 *
 * 예약 도메인(BUY-0x 등) 전체는 아직 구현되지 않았지만, V1 마이그레이션에 reservation 테이블은
 * 이미 존재한다. 이번 티켓(SEL-01~04)의 "상품 삭제 시 진행 중인 예약이 있으면 거부" 요구사항을
 * 충족하기 위해 조회에 필요한 컬럼만 최소로 매핑했다. 예약 등록/취소 등 나머지 필드/기능은
 * 예약 도메인 티켓에서 채워질 예정이다.
 * * [추가] 판매자 예약 관리 및 대시보드(SEL-05, 06) 기능을 위해
 * 판매자/구매자 정보, 시간 컬럼, 상태 변경 메서드를 추가로 매핑함.
 */
@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // --- 👇 이번 티켓에서 추가된 예약 상세 필드들 👇 ---
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    // ------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.REQUESTED; // 객체 생성 시 기본 상태

    public enum Status {
        REQUESTED, APPROVED, REJECTED, CANCELED, COMPLETED, NO_SHOW
    }

    // --- 👇 이번 티켓에서 추가된 상태 변경 메서드들 👇 ---
    public void approve() {
        this.status = Status.APPROVED;
    }

    public void reject(String reason) {
        this.status = Status.REJECTED;
        this.rejectReason = reason;
    }

    public void complete() {
        this.status = Status.COMPLETED;
    }

    public void markAsNoShow() {
        this.status = Status.NO_SHOW;
    }
}