package com.jejulocaltime.api.repository;

import com.jejulocaltime.api.domain.SellerApplication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    
    // 특정 유저의 입점 신청 내역 조회
    Optional<SellerApplication> findByUserId(Long userId);
    
    // 이미 신청한 내역이 있는지 중복 검사용
    boolean existsByUserId(Long userId);

    boolean existsByBusinessNumberAndUserIdNot(String businessNumber, Long userId);
}
