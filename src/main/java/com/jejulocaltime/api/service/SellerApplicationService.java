package com.jejulocaltime.api.service;

import com.jejulocaltime.api.domain.SellerApplication;
import com.jejulocaltime.api.domain.SellerProfile;
import com.jejulocaltime.api.domain.User;
import com.jejulocaltime.api.dto.SellerApplicationDto;
import com.jejulocaltime.api.repository.SellerApplicationRepository;
import com.jejulocaltime.api.repository.SellerProfileRepository;
import com.jejulocaltime.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerApplicationService {

    private final SellerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository profileRepository;

    /**
     * [판매자] 입점 신청서 등록
     * @param userId 현재 로그인한 유저의 PK
     * @param request 입점 신청 정보 (상호명, 사업자번호 등)
     */
    @Transactional
    public SellerApplicationDto.Response createApplication(Long userId, SellerApplicationDto.CreateRequest request) {
        
        // 1. 중복 신청 방지 (한 유저당 하나의 신청 내역만 존재하도록 제약)
        var existing = applicationRepository.findByUserId(userId);
        if (existing.isPresent()) {
            approveAndProvision(existing.get());
            return SellerApplicationDto.Response.from(existing.get());
        }

        // 2. 엔티티 생성 및 데이터 세팅 (기본 status는 PENDING으로 설정됨)
        SellerApplication application = new SellerApplication();
        application.setUserId(userId);
        application.setBusinessName(request.businessName());
        application.setBusinessNumber(request.businessNumber());
        application.setRepresentativeName(request.representativeName());
        application.setBusinessDocumentUrl(request.businessDocumentUrl() == null ? "" : request.businessDocumentUrl());

        // 3. DB 저장
        SellerApplication saved = applicationRepository.save(application);
        approveAndProvision(saved);

        return SellerApplicationDto.Response.from(saved);
    }

    /** 개발/현재 서비스 정책: 사업자 정보를 설정하면 즉시 판매자 권한과 프로필을 생성한다. */
    private void approveAndProvision(SellerApplication application) {
        if (application.getStatus() != SellerApplication.ApplicationStatus.APPROVED) application.approve();
        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.promoteToSeller();
        if (!profileRepository.existsByUserId(application.getUserId())) {
            SellerProfile profile = new SellerProfile();
            profile.setUserId(application.getUserId());
            profile.setSellerApplicationId(application.getId());
            profile.setBusinessName(application.getBusinessName());
            profile.setBusinessNumber(application.getBusinessNumber());
            profile.setRepresentativeName(application.getRepresentativeName());
            profile.setAccountHolder(application.getRepresentativeName());
            profile.setVerificationStatus(SellerProfile.VerificationStatus.VERIFIED);
            profileRepository.save(profile);
        }
    }

    /**
     * [판매자] 본인의 입점 신청 상태 조회
     * @param userId 현재 로그인한 유저의 PK
     */
    public SellerApplicationDto.Response getMyApplication(Long userId) {
        SellerApplication application = applicationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("입점 신청 내역이 없습니다."));
        return SellerApplicationDto.Response.from(application);
    }

    /**
     * [관리자 전용] 입점 신청 승인 및 권한 승격
     * @param applicationId 승인할 신청서의 ID
     */
    @Transactional
    public SellerApplicationDto.Response approveApplication(Long applicationId) {
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입점 신청서를 찾을 수 없습니다."));

        // 대기 상태(PENDING)의 신청서만 승인 가능
        if (application.getStatus() != SellerApplication.ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기(PENDING) 상태인 신청서만 승인할 수 있습니다.");
        }

        // 1. 신청서 상태를 'APPROVED'로 변경
        application.approve();

        // 2. 해당 신청서를 제출한 유저를 찾아 권한을 'SELLER'로 승격 (JPA Dirty Checking으로 자동 반영)
        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        user.promoteToSeller();

        return SellerApplicationDto.Response.from(application);
    }

    /**
     * [관리자 전용] 입점 신청 반려
     * @param applicationId 반려할 신청서의 ID
     * @param reason 반려 사유
     */
    @Transactional
    public SellerApplicationDto.Response rejectApplication(Long applicationId, String reason) {
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입점 신청서를 찾을 수 없습니다."));

        // 대기 상태(PENDING)의 신청서만 반려 가능
        if (application.getStatus() != SellerApplication.ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기(PENDING) 상태인 신청서만 반려할 수 있습니다.");
        }

        // 상태를 'REJECTED'로 변경하고 반려 사유 기록
        application.reject(reason);

        return SellerApplicationDto.Response.from(application);
    }
}
