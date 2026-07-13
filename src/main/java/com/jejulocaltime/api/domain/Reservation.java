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

/**
 * 예약 도메인의 최소 매핑.
 *
 * 예약 도메인(BUY-0x 등) 전체는 아직 구현되지 않았지만, V1 마이그레이션에 reservation 테이블은
 * 이미 존재한다. 이번 티켓(SEL-01~04)의 "상품 삭제 시 진행 중인 예약이 있으면 거부" 요구사항을
 * 충족하기 위해 조회에 필요한 컬럼만 최소로 매핑했다. 예약 등록/취소 등 나머지 필드/기능은
 * 예약 도메인 티켓에서 채워질 예정이다.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        REQUESTED, APPROVED, REJECTED, CANCELED, COMPLETED, NO_SHOW
    }
}
