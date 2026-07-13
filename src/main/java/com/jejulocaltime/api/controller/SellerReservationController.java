package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.dto.SellerReservationDto;
import com.jejulocaltime.api.service.SellerReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/reservations")
@RequiredArgsConstructor
public class SellerReservationController {

    private final SellerReservationService reservationService;

    // 1. 예약 요청 목록 조회 (GET)
    @GetMapping
    public ResponseEntity<List<SellerReservationDto.Response>> getReservations(
            @AuthenticationPrincipal Long sellerId,
            @RequestParam(required = false) String status) {
        
        List<SellerReservationDto.Response> response = reservationService.getReservations(sellerId, status);
        return ResponseEntity.ok(response);
    }

    // 2. 예약 승인 (PATCH)
    @PatchMapping("/{reservationId}/approve")
    public ResponseEntity<SellerReservationDto.Response> approveReservation(
            @AuthenticationPrincipal Long sellerId,
            @PathVariable Long reservationId) {
        
        SellerReservationDto.Response response = reservationService.approveReservation(sellerId, reservationId);
        return ResponseEntity.ok(response);
    }

    // 3. 예약 거절 (PATCH)
    @PatchMapping("/{reservationId}/reject")
    public ResponseEntity<SellerReservationDto.Response> rejectReservation(
            @AuthenticationPrincipal Long sellerId,
            @PathVariable Long reservationId,
            @RequestBody(required = false) SellerReservationDto.RejectRequest request) {
        
        SellerReservationDto.Response response = reservationService.rejectReservation(sellerId, reservationId, request);
        return ResponseEntity.ok(response);
    }

    // 4. 예약 방문완료 처리 (PATCH)
    @PatchMapping("/{reservationId}/complete")
    public ResponseEntity<SellerReservationDto.Response> completeReservation(
            @AuthenticationPrincipal Long sellerId,
            @PathVariable Long reservationId) {
        
        SellerReservationDto.Response response = reservationService.completeReservation(sellerId, reservationId);
        return ResponseEntity.ok(response);
    }

    // 5. 예약 노쇼 처리 (PATCH)
    @PatchMapping("/{reservationId}/no-show")
    public ResponseEntity<SellerReservationDto.Response> noShowReservation(
            @AuthenticationPrincipal Long sellerId,
            @PathVariable Long reservationId) {
        
        SellerReservationDto.Response response = reservationService.noShowReservation(sellerId, reservationId);
        return ResponseEntity.ok(response);
    }
}