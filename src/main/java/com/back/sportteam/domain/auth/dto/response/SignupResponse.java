package com.back.sportteam.domain.auth.dto.response;

import com.back.sportteam.auth.domain.AuthProvider;
import com.back.sportteam.user.domain.User;
import com.back.sportteam.user.domain.UserRole;

public record SignupResponse(
        Long userId,
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