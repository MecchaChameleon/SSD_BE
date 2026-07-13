package com.jejulocaltime.api.dto;

public class SellerDashboardDto {

    // [GET] 대시보드 응답용 Record
    public record Response(
            long productCount,     // 당일 예약이 발생한 상품(종류) 수
            long reservationCount, // 당일 총 예약 건수
            long confirmedCount,   // 당일 확정/완료 건수
            long noshowCount,      // 당일 노쇼 건수
            long revenue,           // 당일 총 매출액
            long unsettledAmount   // [추가] 미정산 누적 금액 (수수료 제외)
    ) {}
}