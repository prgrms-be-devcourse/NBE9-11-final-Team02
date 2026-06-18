package com.back.sportteam.domain.auth.dto.response;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;

public record SignupResponse(
        String userId,
        String email,
        String nickname,
        UserRole role,
        AuthProvider provider
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider()
        );
    }
}