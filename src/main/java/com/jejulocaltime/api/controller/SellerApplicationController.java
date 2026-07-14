package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.dto.SellerApplicationDto;
import com.jejulocaltime.api.service.SellerApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/seller/application")
@RequiredArgsConstructor
@Tag(name = "Seller-Application", description = "판매자 입점 신청 및 신청 상태 조회 API")
public class SellerApplicationController {

    private final SellerApplicationService applicationService;

    // 1. 입점 신청 (POST)
    @PostMapping
    @Operation(summary = "판매자 입점 신청", description = "상호명, 사업자등록번호, 대표자명, 개업일자를 국세청 API로 검증합니다. 검증 통과 시 APPROVED와 SELLER 권한이 부여되고, 실패 시 REJECTED 상태로 저장됩니다.")
    public ResponseEntity<SellerApplicationDto.Response> createApplication(
            @AuthenticationPrincipal Long userId,
            // 테스트를 위해 @Valid 임시 제거
            @RequestBody SellerApplicationDto.CreateRequest request) {

        SellerApplicationDto.Response response = applicationService.createApplication(userId, request);
        return ResponseEntity.ok(response);
    }

    // 2. 내 입점 신청 상태 조회 (GET)
    @GetMapping("/me")
    @Operation(summary = "내 입점 신청 조회", description = "현재 회원의 입점 신청 정보와 승인·반려 상태를 조회합니다.")
    public ResponseEntity<SellerApplicationDto.Response> getMyApplication(
            @AuthenticationPrincipal Long userId) {

        SellerApplicationDto.Response response = applicationService.getMyApplication(userId);
        return ResponseEntity.ok(response);
    }
}
