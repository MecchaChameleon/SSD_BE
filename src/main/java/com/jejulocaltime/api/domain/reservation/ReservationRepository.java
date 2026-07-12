package com.jejulocaltime.api.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByProductIdAndStatusIn(Long productId, Collection<Reservation.Status> statuses);
}
