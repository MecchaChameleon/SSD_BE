package com.jejulocaltime.api.repository;

import com.jejulocaltime.api.domain.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByProductIdAndStatusIn(Long productId, Collection<Reservation.Status> statuses);
}
