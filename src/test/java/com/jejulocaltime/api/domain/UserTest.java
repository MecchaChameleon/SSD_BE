package com.jejulocaltime.api.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void kakaoReloginDoesNotOverwriteStoredProfileImage() {
        User user = new User(1L, "user@example.com", "기존", "https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/custom.jpg");

        user.updateProfile("new@example.com", "새이름", null);

        assertThat(user.getProfileImageUrl()).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/custom.jpg");
        assertThat(user.getNickname()).isEqualTo("새이름");
    }

    @Test
    void kakaoImageIsUsedWhenStoredProfileImageIsMissing() {
        User user = new User(1L, "user@example.com", "사용자", null);

        user.updateProfile("user@example.com", "사용자", "https://k.kakaocdn.net/profile.jpg");

        assertThat(user.getProfileImageUrl()).isEqualTo("https://k.kakaocdn.net/profile.jpg");
    }
}
