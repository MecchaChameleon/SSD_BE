package com.jejulocaltime.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long kakaoId;

    private String email;

    private String nickname;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {
    }

    public User(Long kakaoId, String email, String nickname, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(String email, String nickname, String profileImageUrl) {
        this.email = email;
        this.nickname = nickname;
        // 카카오 재로그인 시 사용자가 업로드해 DB/S3에 저장한 프로필 이미지를 덮어쓰지 않는다.
        // 기존 이미지가 없는 계정에 한해서만 카카오 기본 이미지를 최초 보완한다.
        if (this.profileImageUrl == null || this.profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getKakaoId() {
        return kakaoId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    //  판매자 승격 메서드 추가
    public void promoteToSeller() {
        this.role = Role.SELLER;
    }
    
    //  SELLER 권한 추가
    public enum Role {
        USER, SELLER, ADMIN
    }
}
