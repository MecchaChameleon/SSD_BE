package com.jejulocaltime.api.service;

import com.jejulocaltime.api.auth.JwtTokenProvider;
import com.jejulocaltime.api.domain.User;
import com.jejulocaltime.api.dto.LoginResponse;
import com.jejulocaltime.api.repository.UserRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class OAuthLoginTicketService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long STATE_TTL_MINUTES = 10;
    private static final long TICKET_TTL_MINUTES = 2;

    private final JdbcClient jdbcClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public OAuthLoginTicketService(
            JdbcClient jdbcClient,
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider) {
        this.jdbcClient = jdbcClient;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public String issueState() {
        removeExpiredValues();
        String state = randomValue();
        jdbcClient.sql("INSERT INTO oauth_login_state(state_hash, expires_at) VALUES (:hash, :expiresAt)")
                .param("hash", hash(state))
                .param("expiresAt", Instant.now().plus(STATE_TTL_MINUTES, ChronoUnit.MINUTES))
                .update();
        return state;
    }

    @Transactional
    public void consumeState(String state) {
        int deleted = jdbcClient.sql("""
                        DELETE FROM oauth_login_state
                        WHERE state_hash = :hash AND expires_at > now()
                        """)
                .param("hash", hash(state))
                .update();
        if (deleted != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "만료되었거나 유효하지 않은 로그인 요청입니다.");
        }
    }

    @Transactional
    public String issueTicket(User user, boolean isNewUser) {
        String ticket = randomValue();
        jdbcClient.sql("""
                        INSERT INTO login_ticket(ticket_hash, user_id, is_new_user, expires_at)
                        VALUES (:hash, :userId, :isNewUser, :expiresAt)
                        """)
                .param("hash", hash(ticket))
                .param("userId", user.getId())
                .param("isNewUser", isNewUser)
                .param("expiresAt", Instant.now().plus(TICKET_TTL_MINUTES, ChronoUnit.MINUTES))
                .update();
        return ticket;
    }

    @Transactional
    public LoginResponse exchange(String ticket) {
        try {
            TicketOwner owner = jdbcClient.sql("""
                            DELETE FROM login_ticket
                            WHERE ticket_hash = :hash AND expires_at > now()
                            RETURNING user_id, is_new_user
                            """)
                    .param("hash", hash(ticket))
                    .query((rs, rowNum) -> new TicketOwner(rs.getLong("user_id"), rs.getBoolean("is_new_user")))
                    .single();
            User user = userRepository.findById(owner.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 사용자를 찾을 수 없습니다."));
            String jwt = jwtTokenProvider.createToken(user.getId(), user.getRole().name());
            return LoginResponse.of(jwt, user, owner.isNewUser());
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료되었거나 이미 사용된 로그인 티켓입니다.");
        }
    }

    private void removeExpiredValues() {
        jdbcClient.sql("DELETE FROM oauth_login_state WHERE expires_at <= now()").update();
        jdbcClient.sql("DELETE FROM login_ticket WHERE expires_at <= now()").update();
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private record TicketOwner(long userId, boolean isNewUser) {
    }
}
