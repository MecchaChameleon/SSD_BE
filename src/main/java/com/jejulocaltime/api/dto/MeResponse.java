package com.jejulocaltime.api.dto;

import com.jejulocaltime.api.domain.User;


public record MeResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        String role
) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImageUrl(), user.getRole().name());
    }
}
