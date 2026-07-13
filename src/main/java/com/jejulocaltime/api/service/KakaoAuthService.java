package com.jejulocaltime.api.service;

import com.jejulocaltime.api.auth.InvalidKakaoTokenException;
import com.jejulocaltime.api.auth.JwtTokenProvider;
import com.jejulocaltime.api.domain.User;
import com.jejulocaltime.api.dto.KakaoUserInfoResponse;
import com.jejulocaltime.api.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class KakaoAuthService {

    private final RestClient kakaoApiClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public KakaoAuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.kakaoApiClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public KakaoLoginResult loginWithKakaoAccessToken(String kakaoAccessToken) {
        KakaoUserInfoResponse userInfo = fetchUserInfo(kakaoAccessToken);

        boolean isNewUser = userRepository.findByKakaoId(userInfo.id()).isEmpty();
        User user = userRepository.findByKakaoId(userInfo.id())
                .orElseGet(() -> new User(userInfo.id(), userInfo.email(), resolveNickname(userInfo), userInfo.profileImageUrl()));

        // 카카오 동의항목에서 닉네임 제공에 동의하지 않으면 nickname이 null로 내려올 수 있는데,
        // users.nickname은 NOT NULL이라 그대로 저장하면 DataIntegrityViolationException(500)이 난다.
        // 이번 로그인에서 null이면 기존 닉네임(신규 유저는 resolveNickname의 fallback)을 유지한다.
        String nickname = userInfo.nickname() != null ? userInfo.nickname() : user.getNickname();
        user.updateProfile(userInfo.email(), nickname, userInfo.profileImageUrl());
        user = userRepository.save(user);

        String jwt = jwtTokenProvider.createToken(user.getId(), user.getRole().name());
        return new KakaoLoginResult(jwt, user, isNewUser);
    }

    private String resolveNickname(KakaoUserInfoResponse userInfo) {
        return userInfo.nickname() != null ? userInfo.nickname() : "카카오사용자" + userInfo.id();
    }

    private KakaoUserInfoResponse fetchUserInfo(String kakaoAccessToken) {
        try {
            return kakaoApiClient.get()
                    .uri("/v2/user/me")
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientResponseException e) {
            throw new InvalidKakaoTokenException("유효하지 않은 카카오 액세스 토큰입니다.");
        }
    }

    public record KakaoLoginResult(String jwt, User user, boolean isNewUser) {
    }
}
