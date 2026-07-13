package com.jejulocaltime.api.service;

import com.jejulocaltime.api.domain.Reservation;
import com.jejulocaltime.api.dto.SellerReservationDto;
import com.jejulocaltime.api.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerReservationService {

    private final ReservationRepository reservationRepository;
    private final ProductService productService; // 진짜 상품 서비스 의존성 주입

    // 1. 예약 목록 조회
    public List<SellerReservationDto.Response> getReservations(Long sellerId, String statusQuery) {
        List<Reservation> reservations;

        if (statusQuery != null && !statusQuery.isBlank()) {
            try {
                Reservation.Status statusEnum = Reservation.Status.valueOf(statusQuery.toUpperCase());
                reservations = reservationRepository.findBySellerIdAndStatus(sellerId, statusEnum);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("올바르지 않은 예약 상태 검색어입니다.");
            }
        } else {
            reservations = reservationRepository.findBySellerId(sellerId);
        }

        return reservations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 2. 예약 승인 (실시간 잔여 수량 차감 연동)
    @Transactional
    public SellerReservationDto.Response approveReservation(Long sellerId, Long reservationId) {
        Reservation reservation = getMyReservation(sellerId, reservationId);

        if (reservation.getStatus() != Reservation.Status.REQUESTED) {
            throw new IllegalStateException("대기(REQUESTED) 상태인 예약만 승인할 수 있습니다.");
        }

        reservation.approve();
        productService.decreaseRemainingQuantity(reservation.getProductId()); // 상품의 remainingQuantity -1 

        return toResponse(reservation);
    }

    // 3. 예약 거절
    @Transactional
    public SellerReservationDto.Response rejectReservation(Long sellerId, Long reservationId, SellerReservationDto.RejectRequest request) {
        Reservation reservation = getMyReservation(sellerId, reservationId);

        if (reservation.getStatus() != Reservation.Status.REQUESTED) {
            throw new IllegalStateException("대기(REQUESTED) 상태인 예약만 거절할 수 있습니다.");
        }

        String reason = (request != null && request.reason() != null) ? request.reason() : "가게 사정으로 인해 거절되었습니다.";
        reservation.reject(reason);

        return toResponse(reservation);
    }

    // 4. 예약 방문 완료
    @Transactional
    public SellerReservationDto.Response completeReservation(Long sellerId, Long reservationId) {
        Reservation reservation = getMyReservation(sellerId, reservationId);

        if (reservation.getStatus() != Reservation.Status.APPROVED) {
            throw new IllegalStateException("승인(APPROVED)된 예약만 방문 완료 처리할 수 있습니다.");
        }

        reservation.complete();
        return toResponse(reservation);
    }

    // 5. 예약 노쇼 처리
    @Transactional
    public SellerReservationDto.Response noShowReservation(Long sellerId, Long reservationId) {
        Reservation reservation = getMyReservation(sellerId, reservationId);

        if (reservation.getStatus() != Reservation.Status.APPROVED) {
            throw new IllegalStateException("승인(APPROVED)된 예약만 노쇼 처리할 수 있습니다.");
        }

        reservation.markAsNoShow();
        return toResponse(reservation);
    }

    private Reservation getMyReservation(Long sellerId, Long reservationId) {
        return reservationRepository.findByIdAndSellerId(reservationId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 예약을 찾을 수 없거나 권한이 없습니다."));
    }

    private SellerReservationDto.Response toResponse(Reservation res) {
        return new SellerReservationDto.Response(
                res.getId(),
                res.getStatus().name(),
                res.getCustomerName(),
                res.getRejectReason()
        );
    }
}