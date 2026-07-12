package com.jejulocaltime.api.domain.seller;

import com.jejulocaltime.api.domain.seller.dto.SellerProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/profile")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService profileService;

    // 1. 가게 프로필 최초 등록 (정산/위치 정보 등)
    @PostMapping
    public ResponseEntity<SellerProfileDto.Response> createProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody SellerProfileDto.CreateRequest request) {

        SellerProfileDto.Response response = profileService.createProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // 2. 내 가게 프로필 조회
    @GetMapping
    public ResponseEntity<SellerProfileDto.Response> getProfile(
            @AuthenticationPrincipal Long userId) {

        SellerProfileDto.Response response = profileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    // 3. 내 가게 프로필 수정
    @PutMapping
    public ResponseEntity<SellerProfileDto.Response> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody SellerProfileDto.UpdateRequest request) {

        SellerProfileDto.Response response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}