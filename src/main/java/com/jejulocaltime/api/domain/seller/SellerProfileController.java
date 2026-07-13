package com.jejulocaltime.api.domain.seller;

import com.jejulocaltime.api.domain.seller.dto.SellerProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/seller/profile")
@RequiredArgsConstructor
@Tag(name = "Seller-Profile", description = "판매자 매장 위치 및 정산 정보 관리 API")
public class SellerProfileController {

    private final SellerProfileService profileService;

    // 1. 가게 프로필 최초 등록 (정산/위치 정보 등)
    @PostMapping
    @Operation(summary = "판매자 프로필 등록", description = "승인된 입점 신청에 매장 주소, 좌표, 은행 및 정산계좌 정보를 등록합니다. 이미 프로필이 있으면 전달된 값으로 수정합니다.")
    public ResponseEntity<SellerProfileDto.Response> createProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody SellerProfileDto.CreateRequest request) {

        SellerProfileDto.Response response = profileService.createProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // 2. 내 가게 프로필 조회
    @GetMapping
    @Operation(summary = "내 판매자 프로필 조회", description = "상호명과 사업자 정보, 매장 위치, 은행 및 정산계좌 정보를 모두 조회합니다.")
    public ResponseEntity<SellerProfileDto.Response> getProfile(
            @AuthenticationPrincipal Long userId) {

        SellerProfileDto.Response response = profileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    // 3. 내 가게 프로필 수정
    @PutMapping
    @Operation(summary = "판매자 프로필 수정", description = "매장 주소, 좌표, 은행명, 계좌번호, 예금주를 부분 수정합니다. null 필드는 기존 값을 유지합니다.")
    public ResponseEntity<SellerProfileDto.Response> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody SellerProfileDto.UpdateRequest request) {

        SellerProfileDto.Response response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
