package com.jejulocaltime.api.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String accessToken
) {
}
