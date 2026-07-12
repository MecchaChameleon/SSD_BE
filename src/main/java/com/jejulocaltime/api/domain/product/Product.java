package com.jejulocaltime.api.domain.product;

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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 판매자 상품/자원 (SEL-01~04).
 *
 * V1__create_initial_schema.sql 의 product 테이블에 매핑된다. 이 테이블은 이번 티켓(SEL-01~04)보다
 * 먼저 만들어진 더 풍부한 스키마라서, 아래와 같이 티켓 스펙 필드를 기존 컬럼에 매핑했다.
 * (팀 내 스펙 확정 후 조정 필요하면 이 클래스부터 보면 됨)
 *
 *  - seller_profile_id : User를 직접 FK로 잡지 않고 seller_profile(승인된 판매자 프로필)을 거친다.
 *  - qty(티켓)          : total_quantity/remaining_quantity 두 컬럼으로 분리되어 있음.
 *                         등록 시 둘 다 qty로 초기화하고, 수정 시에는 remaining_quantity(판매 가능 수량)만 갱신한다.
 *  - price(티켓)        : original_price 컬럼에 매핑. current_price는 등록 시 price와 동일하게 초기화하고,
 *                         이후 사장님이 price를 직접 수정하면 함께 갱신한다(AI 추천가 반영은 SEL-03/04 범위 밖).
 *  - resource_type      : 티켓에 없는 컬럼이지만 NOT NULL이라 category로부터 유추해서 채운다
 *                         (ResourceType.fromCategory 참고). TODO: 기획 확정되면 요청 필드로 받는 것으로 변경 검토.
 *  - type(티켓)         : environment_type(INDOOR/OUTDOOR) 컬럼에 매핑.
 *  - openTime/deadline(티켓) : available_start_at / reservation_close_at 컬럼에 매핑.
 *                         available_end_at은 이번 API 범위에서 다루지 않음(null 허용).
 *  - foot(티켓)         : foot_traffic_level 컬럼에 매핑. 컬럼은 LOW/MEDIUM/HIGH까지 허용하지만
 *                         이번 티켓 스펙대로 HIGH/LOW만 API로 노출한다.
 *  - status             : 컬럼은 DRAFT/ACTIVE/PAUSED/SOLD_OUT/CLOSED까지 허용하지만
 *                         이번 티켓 스펙대로 ACTIVE/PAUSED/CLOSED만 API로 노출한다.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_profile_id", nullable = false)
    private Long sellerProfileId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type")
    private EnvironmentType environmentType;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    @Column(name = "minimum_price", nullable = false)
    private Integer minimumPrice;

    @Column(name = "current_price", nullable = false)
    private Integer currentPrice;

    @Column(name = "available_start_at")
    private LocalDateTime availableStartAt;

    @Column(name = "available_end_at")
    private LocalDateTime availableEndAt;

    @Column(name = "reservation_close_at", nullable = false)
    private LocalDateTime reservationCloseAt;

    private String address;

    @Column(precision = 10, scale = 7)
    private Double latitude;

    @Column(precision = 10, scale = 7)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "foot_traffic_level")
    private FootTrafficLevel footTrafficLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isOwnedBy(Long sellerProfileId) {
        return this.sellerProfileId != null && this.sellerProfileId.equals(sellerProfileId);
    }

    /** 티켓 스펙 카테고리 (당일재고/빈시간대자원/당일공실/이동관광잔여상품) */
    public enum Category {
        SAME_DAY_INVENTORY,
        EMPTY_TIME_RESOURCE,
        SAME_DAY_ROOM,
        TOUR_REMAINDER
    }

    /**
     * DB의 resource_type 컬럼(INVENTORY/SEAT/ROOM/EXPERIENCE/TOUR/RENTAL)은 이번 티켓 요청 필드에 없어서
     * category로부터 대표값을 유추한다. TODO: AI/기획 파트와 실제 매핑 확정 필요.
     */
    public enum ResourceType {
        INVENTORY, SEAT, ROOM, EXPERIENCE, TOUR, RENTAL;

        public static ResourceType fromCategory(Category category) {
            return switch (category) {
                case SAME_DAY_INVENTORY -> INVENTORY;
                case EMPTY_TIME_RESOURCE -> EXPERIENCE;
                case SAME_DAY_ROOM -> ROOM;
                case TOUR_REMAINDER -> TOUR;
            };
        }
    }

    public enum EnvironmentType {
        INDOOR, OUTDOOR
    }

    public enum FootTrafficLevel {
        HIGH, LOW
    }

    public enum Status {
        ACTIVE, PAUSED, CLOSED
    }
}
