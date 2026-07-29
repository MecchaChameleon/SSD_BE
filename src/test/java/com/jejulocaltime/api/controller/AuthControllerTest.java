package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.repository.UserRepository;
import com.jejulocaltime.api.service.AccountWithdrawalService;
import com.jejulocaltime.api.service.KakaoAuthService;
import com.jejulocaltime.api.service.OAuthLoginTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private static final String KAKAO_CALLBACK = "https://api.example.com/api/auth/kakao/callback";
    private static final String APP_CALLBACK = "jejulocaltime://oauth/kakao";
    private static final String WEB_CALLBACK = "https://web.example.com/oauth/kakao";

    private KakaoAuthService kakaoAuthService;
    private OAuthLoginTicketService loginTicketService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        kakaoAuthService = mock(KakaoAuthService.class);
        loginTicketService = mock(OAuthLoginTicketService.class);
        controller = new AuthController(
                kakaoAuthService,
                mock(UserRepository.class),
                mock(AccountWithdrawalService.class),
                loginTicketService,
                "rest-api-key",
                KAKAO_CALLBACK,
                APP_CALLBACK,
                WEB_CALLBACK);
    }

    @Test
    void webLoginStoresTheConfiguredWebCallbackInState() {
        when(loginTicketService.issueState(WEB_CALLBACK)).thenReturn("oauth-state");

        ResponseEntity<Void> response = controller.startKakaoLogin("web");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().toString())
                .contains("https://kauth.kakao.com/oauth/authorize")
                .contains("state=oauth-state");
        verify(loginTicketService).issueState(WEB_CALLBACK);
    }

    @Test
    void cancelledWebLoginReturnsToTheStoredWebCallback() {
        when(loginTicketService.consumeState("oauth-state")).thenReturn(WEB_CALLBACK);

        ResponseEntity<Void> response =
                controller.completeKakaoLogin(null, "oauth-state", "access_denied");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString(
                WEB_CALLBACK + "?error=access_denied");
    }

    @Test
    void failedWebLoginReturnsToWebInsteadOfTheAppScheme() {
        when(loginTicketService.consumeState("oauth-state")).thenReturn(WEB_CALLBACK);
        when(kakaoAuthService.loginWithAuthorizationCode("code", KAKAO_CALLBACK))
                .thenThrow(new IllegalStateException("token exchange failed"));

        ResponseEntity<Void> response =
                controller.completeKakaoLogin("code", "oauth-state", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString(
                WEB_CALLBACK + "?error=KAKAO_LOGIN_FAILED");
    }
}
