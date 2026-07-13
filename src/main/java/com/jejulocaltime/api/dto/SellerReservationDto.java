package com.jejulocaltime.api.dto;

public class SellerReservationDto {

    // 예약 거절할 때 프론트에서 보내주는 데이터 [PATCH]
    public record RejectRequest(
            String reason
    ) {}

    // 예약 정보를 반환할 때 사용하는 데이터 [GET, PATCH]
    public record Response(
            Long reservationId,
            String status,
            String customerName,
            String rejectReason
    ) {}
}