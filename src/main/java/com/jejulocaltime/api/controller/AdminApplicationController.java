package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.dto.SellerApplicationDto;
import com.jejulocaltime.api.service.SellerApplicationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/application")
@RequiredArgsConstructor
public class AdminApplicationController {

    private final SellerApplicationService applicationService;

    // 1. 입점 신청 승인
    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<SellerApplicationDto.Response> approve(
            @PathVariable Long applicationId) {
        
        SellerApplicationDto.Response response = applicationService.approveApplication(applicationId);
        return ResponseEntity.ok(response);
    }

    // 2. 입점 신청 반려 (사유 포함)
    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<SellerApplicationDto.Response> reject(
            @PathVariable Long applicationId,
            @RequestBody String reason) {
        
        SellerApplicationDto.Response response = applicationService.rejectApplication(applicationId, reason);
        return ResponseEntity.ok(response);
    }
}