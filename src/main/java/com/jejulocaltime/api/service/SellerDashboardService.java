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

    // --- 👇 수수료 비율 설정 상수화 (요구사항 반영) 👇 ---
    private static final double PLATFORM_COMMISSION_RATE = 0.03; // 플랫폼 수수료 3%
    private static final double PAYMENT_COMMISSION_RATE = 0.02;  // 결제 수수료 2%
    
    // 총 차감 수수료율 (3% + 2% = 5%)
    private static final double TOTAL_COMMISSION_RATE = PLATFORM_COMMISSION_RATE + PAYMENT_COMMISSION_RATE;

    public SellerDashboardDto.Response getDashboardSummary(Long sellerId) {
        // 1. 사장님의 '전체' 예약 목록 조회 (누적 정산금 계산용)
        List<Reservation> allReservations = reservationRepository.findBySellerId(sellerId);

        // 2. '당일'의 기준 시간 세팅 (오늘 00:00:00 ~ 23:59:59)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // 3. 전체 예약 중 '오늘 생성된 예약'만 필터링
        List<Reservation> todayReservations = allReservations.stream()
                .filter(res -> res.getCreatedAt() != null &&
                        !res.getCreatedAt().isBefore(startOfDay) &&
                        !res.getCreatedAt().isAfter(endOfDay))
                .collect(Collectors.toList());

        // 4. 매출 및 정산금 계산을 위해 연관된 모든 상품 ID 추출 (중복 제거)
        List<Long> allProductIds = allReservations.stream()
                .map(Reservation::getProductId)
                .distinct()
                .collect(Collectors.toList());

        // 5. 상품 ID로 가격(Price) 정보 한 번에 맵으로 매핑
        Map<Long, Integer> productPriceMap = productRepository.findAllById(allProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getCurrentPrice));

        // ================= [당일 통계 집계] =================
        long reservationCount = todayReservations.size(); 
        long confirmedCount = 0; 
        long noshowCount = 0;    
        long revenue = 0;        

        for (Reservation res : todayReservations) {
            if (res.getStatus() == Reservation.Status.APPROVED || res.getStatus() == Reservation.Status.COMPLETED) {
                confirmedCount++;
                // 오늘 발생한 매출 총액 (수수료 차감 전 원금)
                revenue += productPriceMap.getOrDefault(res.getProductId(), 0);
            } else if (res.getStatus() == Reservation.Status.NO_SHOW) {
                noshowCount++;
            }
        }
        long productCount = todayReservations.stream().map(Reservation::getProductId).distinct().count();

        // ================= [누적 미정산금 집계] =================
        long totalConfirmedRevenue = 0;

        for (Reservation res : allReservations) {
            // 정산 대상 상태 (APPROVED 혹은 COMPLETED)
            if (res.getStatus() == Reservation.Status.APPROVED || res.getStatus() == Reservation.Status.COMPLETED) {
                totalConfirmedRevenue += productPriceMap.getOrDefault(res.getProductId(), 0);
            }
        }
        
        // 총 누적 매출에서 수수료 5% (3% + 2%)를 뺀 사장님 실수령 정산금 계산
        long unsettledAmount = (long) (totalConfirmedRevenue * (1 - TOTAL_COMMISSION_RATE));

        return new SellerDashboardDto.Response(
                productCount,
                reservationCount,
                confirmedCount,
                noshowCount,
                revenue,
                unsettledAmount
        );
    }
}