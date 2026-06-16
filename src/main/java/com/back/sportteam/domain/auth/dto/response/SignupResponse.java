package com.back.sportteam.domain.auth.dto.response;

import com.back.sportteam.domain.auth.provider.AuthProvider;
<<<<<<< HEAD
import com.back.sportteam.user.domain.User;
import com.back.sportteam.user.domain.UserRole;
import java.util.UUID;
=======
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
>>>>>>> 7f7b23e (refactor: User 엔티티 패키지 이동 (user/domain → domain/user/entity))

public record SignupResponse(
        UUID userId,
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