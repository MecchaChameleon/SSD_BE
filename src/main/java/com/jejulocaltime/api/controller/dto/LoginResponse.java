package com.jejulocaltime.api.controller.dto;

import com.jejulocaltime.api.domain.User;

public record LoginResponse(
        String accessToken,
        Long userId,
        String email,
        String nickname,
        boolean isNewUser
) {

    public static LoginResponse of(String accessToken, User user, boolean isNewUser) {
        return new LoginResponse(accessToken, user.getId(), user.getEmail(), user.getNickname(), isNewUser);
    }
}
