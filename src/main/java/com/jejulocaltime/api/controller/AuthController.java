package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.dto.KakaoLoginRequest;
import com.jejulocaltime.api.dto.LoginTicketRequest;
import com.jejulocaltime.api.dto.LoginResponse;
import com.jejulocaltime.api.dto.MeResponse;
import com.jejulocaltime.api.repository.UserRepository;
import com.jejulocaltime.api.service.KakaoAuthService;
import com.jejulocaltime.api.service.AccountWithdrawalService;
import com.jejulocaltime.api.service.OAuthLoginTicketService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Tag(name = "Auth", description = "카카오 로그인 인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final UserRepository userRepository;
    private final AccountWithdrawalService accountWithdrawalService;
    private final OAuthLoginTicketService loginTicketService;
    private final String kakaoRestApiKey;
    private final String kakaoRedirectUri;
    private final String appLoginRedirectUri;

    public AuthController(KakaoAuthService kakaoAuthService, UserRepository userRepository,
                          AccountWithdrawalService accountWithdrawalService,
                          OAuthLoginTicketService loginTicketService,
                          @Value("${kakao.rest-api-key:}") String kakaoRestApiKey,
                          @Value("${kakao.redirect-uri}") String kakaoRedirectUri,
                          @Value("${kakao.app-login-redirect-uri:jejulocaltime://oauth/kakao}") String appLoginRedirectUri) {
        this.kakaoAuthService = kakaoAuthService;
        this.userRepository = userRepository;
        this.accountWithdrawalService = accountWithdrawalService;
        this.loginTicketService = loginTicketService;
        this.kakaoRestApiKey = kakaoRestApiKey;
        this.kakaoRedirectUri = kakaoRedirectUri;
        this.appLoginRedirectUri = appLoginRedirectUri;
    }

    @GetMapping("/kakao/start")
    public ResponseEntity<Void> startKakaoLogin() {
        if (kakaoRestApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 로그인이 설정되지 않았습니다.");
        }
        String state = loginTicketService.issueState();
        URI location = UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoRestApiKey)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> completeKakaoLogin(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null || code == null || state == null) {
            return redirectToApp("error", error == null ? "KAKAO_LOGIN_CANCELLED" : error);
        }
        try {
            loginTicketService.consumeState(state);
            KakaoAuthService.KakaoLoginResult result =
                    kakaoAuthService.loginWithAuthorizationCode(code, kakaoRedirectUri);
            String ticket = loginTicketService.issueTicket(result.user(), result.isNewUser());
            return redirectToApp("ticket", ticket);
        } catch (RuntimeException e) {
            return redirectToApp("error", "KAKAO_LOGIN_FAILED");
        }
    }

    @PostMapping("/kakao/ticket")
    public LoginResponse exchangeKakaoTicket(@Valid @RequestBody LoginTicketRequest request) {
        return loginTicketService.exchange(request.ticket());
    }

    private ResponseEntity<Void> redirectToApp(String name, String value) {
        URI location = UriComponentsBuilder.fromUriString(appLoginRedirectUri)
                .queryParam(name, value)
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .location(location)
                .build();
    }

    @PostMapping("/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 액세스 토큰을 검증하고 서비스 JWT와 회원 정보를 반환합니다. 최초 로그인 회원은 isNewUser=true로 반환됩니다.")
    public LoginResponse loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        KakaoAuthService.KakaoLoginResult result = kakaoAuthService.loginWithKakaoAccessToken(request.accessToken());
        return LoginResponse.of(result.jwt(), result.user(), result.isNewUser());
    }

    @GetMapping("/me")
    @Operation(summary = "내 회원 정보 조회", description = "서비스 JWT로 인증된 현재 회원의 기본 프로필과 권한을 조회합니다.")
    public MeResponse me(@AuthenticationPrincipal Long userId) {
        return userRepository.findById(userId)
                .map(MeResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "현재 회원과 연관된 판매자, 상품, 결제 내역 데이터를 함께 삭제합니다. 성공 시 응답 본문 없이 204를 반환합니다.")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long userId) {
        accountWithdrawalService.withdraw(userId);
    }
}
