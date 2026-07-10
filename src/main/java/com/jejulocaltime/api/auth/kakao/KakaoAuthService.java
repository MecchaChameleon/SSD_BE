package com.jejulocaltime.api.auth.kakao;

import com.jejulocaltime.api.auth.JwtTokenProvider;
import com.jejulocaltime.api.domain.User;
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
                .orElseGet(() -> new User(userInfo.id(), userInfo.email(), userInfo.nickname(), userInfo.profileImageUrl()));
        user.updateProfile(userInfo.email(), userInfo.nickname(), userInfo.profileImageUrl());
        user = userRepository.save(user);

        String jwt = jwtTokenProvider.createToken(user.getId(), user.getRole().name());
        return new KakaoLoginResult(jwt, user, isNewUser);
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
