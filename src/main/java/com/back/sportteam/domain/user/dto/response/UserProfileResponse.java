package com.back.sportteam.domain.user.dto.response;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String nickname,
        UserRole role,
        AuthProvider provider,
        String profileImg,
        String position,
        String activeRegion,
        String preferredSport,
        Double mannerScore,
        Double skillScore
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getProfileImg(),
                user.getPosition(),
                user.getActiveRegion(),
                user.getPreferredSport(),
                user.getMannerScore(),
                user.getSkillScore()
        );
    }
}