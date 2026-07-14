package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.common.util.BusinessNumberUtils;
import com.jejulocaltime.api.domain.SellerApplication;
import com.jejulocaltime.api.domain.User;
import com.jejulocaltime.api.dto.SellerApplicationDto;
import com.jejulocaltime.api.repository.SellerApplicationRepository;
import com.jejulocaltime.api.repository.SellerProfileRepository;
import com.jejulocaltime.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerApplicationService {

    private final SellerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository profileRepository;
    private final BusinessVerificationClient businessVerificationClient;

    /**
     * [판매자] 입점 신청서 등록
     * 국세청 진위확인 + 상태조회를 통과한 경우에만 APPROVED 및 SELLER 권한을 부여한다.
     * (현재 로컬 테스트를 위해 무조건 승인되도록 국세청 API 우회 처리됨)
     */
    @Transactional
    public SellerApplicationDto.Response createApplication(Long userId, SellerApplicationDto.CreateRequest request) {
        String businessNumber = normalizeAndValidateBusinessNumber(request.businessNumber());
        String openDate = normalizeAndValidateOpenDate(request.openDate());

        var existing = applicationRepository.findByUserId(userId);
        if (existing.isPresent() && existing.get().getStatus() == SellerApplication.ApplicationStatus.APPROVED) {
            return SellerApplicationDto.Response.from(existing.get());
        }

        ensureBusinessNumberAvailable(businessNumber, userId);

        SellerApplication application = existing.orElseGet(SellerApplication::new);
        application.setUserId(userId);
        application.setBusinessName(request.businessName().trim());
        application.setBusinessNumber(businessNumber);
        application.setOpenDate(openDate);
        application.setRepresentativeName(request.representativeName().trim());
        application.setBusinessDocumentUrl(request.businessDocumentUrl() == null ? "" : request.businessDocumentUrl());
        
        // -------------------------------------------------------------
        // [테스트용 임시 주석 처리] 국세청 API 검증 로직 비활성화
        // -------------------------------------------------------------
        /*
        application.setStatus(SellerApplication.ApplicationStatus.PENDING);
        application.setRejectionReason(null);
        application.setNtsVerifiedAt(null);
        application.setNtsBusinessStatus(null);

        BusinessVerificationClient.VerificationResult verification = businessVerificationClient.verify(
                businessNumber, openDate, application.getRepresentativeName());

        if (!verification.valid()) {
            application.reject(verification.validMessage());
            return SellerApplicationDto.Response.from(applicationRepository.save(application));
        }

        if (!verification.isActiveBusiness()) {
            String reason = buildInactiveBusinessReason(verification);
            application.reject(reason);
            return SellerApplicationDto.Response.from(applicationRepository.save(application));
        }
        */

        // -------------------------------------------------------------
        // [테스트용 강제 승인 로직] 무조건 승인 및 판매자 권한 부여
        // -------------------------------------------------------------
        application.approve(); // 무조건 APPROVED 상태로 변경
        application.setRejectionReason(null);
        application.setNtsVerifiedAt(OffsetDateTime.now());
        application.setNtsBusinessStatus("계속사업자(테스트 프리패스)"); // 가짜 상태값 주입

        applicationRepository.save(application);
        promoteToSeller(userId); // 판매자 권한 즉시 부여

        return SellerApplicationDto.Response.from(application);
    }

    /**
     * [판매자] 본인의 입점 신청 상태 조회
     */
    public SellerApplicationDto.Response getMyApplication(Long userId) {
        SellerApplication application = applicationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("입점 신청 내역이 없습니다."));
        return SellerApplicationDto.Response.from(application);
    }

    /**
     * [관리자 전용] 입점 신청 승인 및 권한 승격 (국세청 API 장애 등 수동 처리용)
     */
    @Transactional
    public SellerApplicationDto.Response approveApplication(Long applicationId) {
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입점 신청서를 찾을 수 없습니다."));

        if (application.getStatus() != SellerApplication.ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기(PENDING) 상태인 신청서만 승인할 수 있습니다.");
        }

        application.approve();
        promoteToSeller(application.getUserId());

        return SellerApplicationDto.Response.from(application);
    }

    /**
     * [관리자 전용] 입점 신청 반려
     */
    @Transactional
    public SellerApplicationDto.Response rejectApplication(Long applicationId, String reason) {
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입점 신청서를 찾을 수 없습니다."));

        if (application.getStatus() != SellerApplication.ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기(PENDING) 상태인 신청서만 반려할 수 있습니다.");
        }

        application.reject(reason);

        return SellerApplicationDto.Response.from(application);
    }

    private void promoteToSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.promoteToSeller();
    }

    private void ensureBusinessNumberAvailable(String businessNumber, Long userId) {
        if (profileRepository.existsByBusinessNumber(businessNumber)
                || applicationRepository.existsByBusinessNumberAndUserIdNot(businessNumber, userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_ALREADY_REGISTERED);
        }
    }

    private String normalizeAndValidateBusinessNumber(String businessNumber) {
        if (!BusinessNumberUtils.isValidFormat(businessNumber)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "사업자등록번호는 10자리 숫자여야 합니다.");
        }
        return BusinessNumberUtils.normalize(businessNumber);
    }

    private String normalizeAndValidateOpenDate(String openDate) {
        if (!BusinessNumberUtils.isValidOpenDate(openDate)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "개업일자는 YYYYMMDD 형식이어야 합니다.");
        }
        return BusinessNumberUtils.normalizeOpenDate(openDate);
    }

    private String buildInactiveBusinessReason(BusinessVerificationClient.VerificationResult verification) {
        String statusName = verification.businessStatusName() != null
                ? verification.businessStatusName()
                : "확인 불가";
        return "휴업 또는 폐업 상태의 사업자입니다. (국세청 상태: " + statusName + ")";
    }
}