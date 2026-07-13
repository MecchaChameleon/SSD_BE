package com.jejulocaltime.api.repository;

import com.jejulocaltime.api.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByProductIdAndStatusIn(Long productId, Collection<Reservation.Status> statuses);

    // 예약 관리에 필요한 메서드들
    List<Reservation> findBySellerId(Long sellerId);
    
    List<Reservation> findBySellerIdAndStatus(Long sellerId, Reservation.Status status);
    
    Optional<Reservation> findByIdAndSellerId(Long id, Long sellerId);
}