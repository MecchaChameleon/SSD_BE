package com.jejulocaltime.api.domain.seller;

import com.jejulocaltime.api.domain.seller.dto.SellerApplicationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/application")
@RequiredArgsConstructor
public class SellerApplicationController {

    private final SellerApplicationService applicationService;

    // 1. 입점 신청 (POST)
    @PostMapping
    public ResponseEntity<SellerApplicationDto.Response> createApplication(
            @AuthenticationPrincipal Long userId,
            @RequestBody SellerApplicationDto.CreateRequest request) {

        SellerApplicationDto.Response response = applicationService.createApplication(userId, request);
        return ResponseEntity.ok(response);
    }

    // 2. 내 입점 신청 상태 조회 (GET)
    @GetMapping("/me")
    public ResponseEntity<SellerApplicationDto.Response> getMyApplication(
            @AuthenticationPrincipal Long userId) {

        SellerApplicationDto.Response response = applicationService.getMyApplication(userId);
        return ResponseEntity.ok(response);
    }
}