package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.util.NumberConversions;
import com.jejulocaltime.api.domain.SellerApplication;
import com.jejulocaltime.api.domain.SellerProfile;
import com.jejulocaltime.api.dto.SellerProfileDto;
import com.jejulocaltime.api.repository.SellerApplicationRepository;
import com.jejulocaltime.api.repository.SellerProfileRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProfileService {

    private final SellerProfileRepository profileRepository;
    private final SellerApplicationRepository applicationRepository;

    @Transactional
    public SellerProfileDto.Response createProfile(Long userId, SellerProfileDto.CreateRequest request) {
        
        // 1. 이미 프로필이 있는지 확인
        if (profileRepository.existsByUserId(userId)) {
            return updateProfile(userId, new SellerProfileDto.UpdateRequest(
                    null, null, request.address(), request.latitude(), request.longitude(), request.bankName(), request.accountNumber(), request.accountHolder()));
        }

        // 2. 내 입점 신청서가 '승인(APPROVED)' 상태인지 확인
        SellerApplication application = applicationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("입점 신청 내역이 없습니다."));

        if (application.getStatus() != SellerApplication.ApplicationStatus.APPROVED) {
            throw new IllegalStateException("아직 관리자의 입점 승인이 완료되지 않았습니다.");
        }

        // 3. 프로필 생성 (고정 정보는 Application에서 복사, 나머지는 Request에서 세팅)
        SellerProfile profile = new SellerProfile();
        profile.setUserId(userId);
        profile.setSellerApplicationId(application.getId());
        
        // 승인된 정보 복사 (변조 불가)
        profile.setBusinessName(application.getBusinessName());
        profile.setBusinessNumber(application.getBusinessNumber());
        profile.setRepresentativeName(application.getRepresentativeName());
        
        // 사장님 입력 정보 세팅
        profile.setAddress(request.address());
        profile.setLatitude(NumberConversions.toBigDecimal(request.latitude()));
        profile.setLongitude(NumberConversions.toBigDecimal(request.longitude()));
        profile.setBankName(request.bankName());
        profile.setAccountNumber(request.accountNumber());
        profile.setAccountHolder(request.accountHolder());

        SellerProfile saved = profileRepository.save(profile);
        return SellerProfileDto.Response.from(saved);
    }

    public SellerProfileDto.Response getProfile(Long userId) {
        SellerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("가게 프로필을 찾을 수 없습니다."));
        return SellerProfileDto.Response.from(profile);
    }

    @Transactional
    public SellerProfileDto.Response updateProfile(Long userId, SellerProfileDto.UpdateRequest request) {
        SellerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("가게 프로필을 찾을 수 없습니다."));

        // 프론트엔드에서 null이 아닌 값을 보냈을 때만 부분 수정(Dirty Checking)
        if (request.businessName() != null) profile.setBusinessName(request.businessName());
        if (request.businessNumber() != null) profile.setBusinessNumber(request.businessNumber());
        if (request.address() != null) profile.setAddress(request.address());
        if (request.latitude() != null) profile.setLatitude(NumberConversions.toBigDecimal(request.latitude()));
        if (request.longitude() != null) profile.setLongitude(NumberConversions.toBigDecimal(request.longitude()));
        if (request.bankName() != null) profile.setBankName(request.bankName());
        if (request.accountNumber() != null) profile.setAccountNumber(request.accountNumber());
        if (request.accountHolder() != null) profile.setAccountHolder(request.accountHolder());

        applicationRepository.findById(profile.getSellerApplicationId()).ifPresent(application -> {
            if (request.businessName() != null) application.setBusinessName(request.businessName());
            if (request.businessNumber() != null) application.setBusinessNumber(request.businessNumber());
        });

        return SellerProfileDto.Response.from(profile);
    }
}
