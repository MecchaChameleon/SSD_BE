package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.auth.kakao.KakaoAuthService;
import com.jejulocaltime.api.controller.dto.KakaoLoginRequest;
import com.jejulocaltime.api.controller.dto.LoginResponse;
import com.jejulocaltime.api.controller.dto.MeResponse;
import com.jejulocaltime.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Auth", description = "카카오 로그인 인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final UserRepository userRepository;

    public AuthController(KakaoAuthService kakaoAuthService, UserRepository userRepository) {
        this.kakaoAuthService = kakaoAuthService;
        this.userRepository = userRepository;
    }

    @PostMapping("/kakao")
    public LoginResponse loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        KakaoAuthService.KakaoLoginResult result = kakaoAuthService.loginWithKakaoAccessToken(request.accessToken());
        return LoginResponse.of(result.jwt(), result.user(), result.isNewUser());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return userRepository.findById(userId)
                .map(MeResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    @DeleteMapping("/me")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        userRepository.deleteById(userId);
    }
}
