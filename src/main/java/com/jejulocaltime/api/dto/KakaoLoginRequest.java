package com.jejulocaltime.api.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String accessToken
) {
}
