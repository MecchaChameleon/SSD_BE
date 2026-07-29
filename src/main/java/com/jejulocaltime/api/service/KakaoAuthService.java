package com.jejulocaltime.api.service;

import com.jejulocaltime.api.auth.InvalidKakaoTokenException;
import com.jejulocaltime.api.auth.JwtTokenProvider;
import com.jejulocaltime.api.domain.User;
import com.jejulocaltime.api.dto.KakaoUserInfoResponse;
import com.jejulocaltime.api.repository.UserRepository;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class KakaoAuthService {

    private final RestClient kakaoApiClient;
    private final RestClient kakaoAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String restApiKey;
    private final String clientSecret;

    public KakaoAuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${kakao.rest-api-key:}") String restApiKey,
            @Value("${kakao.client-secret:}") String clientSecret) {
        this.kakaoApiClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
        this.kakaoAuthClient = RestClient.builder()
                .baseUrl("https://kauth.kakao.com")
                .build();
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restApiKey = restApiKey;
        this.clientSecret = clientSecret;
    }

    public KakaoLoginResult loginWithAuthorizationCode(String code, String redirectUri) {
        if (restApiKey.isBlank()) {
            throw new IllegalStateException("KAKAO_REST_API_KEY가 설정되지 않았습니다.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", restApiKey);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (!clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        try {
            KakaoTokenResponse token = kakaoAuthClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new InvalidKakaoTokenException("카카오 액세스 토큰을 발급받지 못했습니다.");
            }
            return loginWithKakaoAccessToken(token.accessToken());
        } catch (RestClientResponseException e) {
            throw new InvalidKakaoTokenException("카카오 인가 코드를 토큰으로 교환하지 못했습니다.");
        }
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

    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}
