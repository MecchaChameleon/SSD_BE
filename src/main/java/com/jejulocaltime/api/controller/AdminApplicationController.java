package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.dto.SellerApplicationDto;
import com.jejulocaltime.api.service.SellerApplicationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/application")
@RequiredArgsConstructor
@Tag(name = "Admin-Application", description = "관리자용 판매자 입점 심사 API")
public class AdminApplicationController {

    private final SellerApplicationService applicationService;

    // 1. 입점 신청 승인
    @PostMapping("/{applicationId}/approve")
    @Operation(summary = "입점 신청 승인", description = "대기 상태의 입점 신청을 승인하고 해당 회원을 판매자 권한으로 변경합니다.")
    public ResponseEntity<SellerApplicationDto.Response> approve(
            @PathVariable Long applicationId) {
        
        SellerApplicationDto.Response response = applicationService.approveApplication(applicationId);
        return ResponseEntity.ok(response);
    }

    // 2. 입점 신청 반려 (사유 포함)
    @PostMapping("/{applicationId}/reject")
    @Operation(summary = "입점 신청 반려", description = "대기 상태의 입점 신청을 사유와 함께 반려합니다.")
    public ResponseEntity<SellerApplicationDto.Response> reject(
            @PathVariable Long applicationId,
            @RequestBody String reason) {
        
        SellerApplicationDto.Response response = applicationService.rejectApplication(applicationId, reason);
        return ResponseEntity.ok(response);
    }
}
