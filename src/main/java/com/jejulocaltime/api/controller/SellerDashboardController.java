package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.dto.SellerDashboardDto;
import com.jejulocaltime.api.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/dashboard")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerDashboardService dashboardService;

    // 대시보드 통계 조회 (GET)
    @GetMapping
    public ResponseEntity<SellerDashboardDto.Response> getDashboard(
            @AuthenticationPrincipal Long sellerId) {

        SellerDashboardDto.Response response = dashboardService.getDashboardSummary(sellerId);
        return ResponseEntity.ok(response);
    }
}