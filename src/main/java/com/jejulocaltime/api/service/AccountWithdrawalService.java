package com.jejulocaltime.api.service;

import com.jejulocaltime.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountWithdrawalService {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    public AccountWithdrawalService(UserRepository userRepository, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public void withdraw(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        Long pendingPayments = jdbc.queryForObject("""
                SELECT count(*)
                  FROM reservation r
                  JOIN product p ON p.id = r.product_id
                  JOIN seller_profile sp ON sp.id = p.seller_profile_id
                 WHERE r.status = 'REQUESTED'
                   AND r.payment_status = 'PAID'
                   AND (r.user_id = ? OR sp.user_id = ?)
                """, Long.class, userId, userId);
        if (pendingPayments != null && pendingPayments > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "판매자 확인 대기 중인 결제가 있습니다. 결제 처리 완료 후 탈퇴해주세요.");
        }

        userRepository.deleteById(userId);
        userRepository.flush();
    }
}
