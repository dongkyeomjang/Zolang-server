package com.kcs.zolang.dto.response;

import com.kcs.zolang.domain.User;
import com.kcs.zolang.dto.type.EProvider;
import com.kcs.zolang.dto.type.ERole;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserCICDDto(
        Long id,
        String serialId,
        String password,
        EProvider provider,
        ERole role,
        LocalDateTime createdAt,
        String nickname,
        String profileImage,
        String email,
        Boolean isLogin,
        String refreshToken,
        String githubAccessToken
) {
    public static UserCICDDto fromEntity(User user) {
        return UserCICDDto.builder()
                .id(user.getId())
                .serialId(user.getSerialId())
                .password(user.getPassword())
                .provider(user.getProvider())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .email(user.getEmail())
                .isLogin(user.getIsLogin())
                .refreshToken(user.getRefreshToken())
                .githubAccessToken(user.getGithubAccessToken())
                .build();
    }
}
