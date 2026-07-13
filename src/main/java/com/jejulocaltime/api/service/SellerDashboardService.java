package com.jejulocaltime.api.service;

import com.jejulocaltime.api.dto.SellerDashboardDto;
import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.domain.Reservation;
import com.jejulocaltime.api.repository.ProductRepository;
import com.jejulocaltime.api.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    public SellerDashboardDto.Response getDashboardSummary(Long sellerId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Reservation> todayReservations = reservationRepository.findBySellerId(sellerId).stream()
                .filter(res -> res.getCreatedAt() != null &&
                        !res.getCreatedAt().isBefore(startOfDay) &&
                        !res.getCreatedAt().isAfter(endOfDay))
                .collect(Collectors.toList());

        List<Long> productIds = todayReservations.stream()
                .map(Reservation::getProductId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Integer> productPriceMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getCurrentPrice));

        long reservationCount = todayReservations.size(); 
        long confirmedCount = 0; 
        long noshowCount = 0;    
        long revenue = 0;        

        for (Reservation res : todayReservations) {
            if (res.getStatus() == Reservation.Status.APPROVED || res.getStatus() == Reservation.Status.COMPLETED) {
                confirmedCount++;
                revenue += productPriceMap.getOrDefault(res.getProductId(), 0);
            } else if (res.getStatus() == Reservation.Status.NO_SHOW) {
                noshowCount++;
            }
        }

        long productCount = productIds.size(); 

        return new SellerDashboardDto.Response(
                productCount,
                reservationCount,
                confirmedCount,
                noshowCount,
                revenue
        );
    }
}