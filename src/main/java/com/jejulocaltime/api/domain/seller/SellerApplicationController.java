package com.jejulocaltime.api.domain.seller;

import com.jejulocaltime.api.domain.seller.dto.SellerApplicationDto;
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
    @Operation(summary = "판매자 입점 신청", description = "상호명, 사업자등록번호, 대표자 정보를 등록합니다. 현재 정책에서는 신청 즉시 승인되고 판매자 프로필이 생성됩니다.")
    public ResponseEntity<SellerApplicationDto.Response> createApplication(
            @AuthenticationPrincipal Long userId,
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
