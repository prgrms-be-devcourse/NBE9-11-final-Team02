package com.back.sportteam.domain.user.dto.response;

import com.back.sportteam.user.domain.User;
import java.util.UUID;

public record UserProfileUpdateResponse(
        UUID userId,
        String nickname,
        String position,
        String activeRegion,
        String preferredSport,
        String profileImg
) {
    public static UserProfileUpdateResponse from(User user) {
        return new UserProfileUpdateResponse(
                user.getId(),
                user.getNickname(),
                user.getPosition(),
                user.getActiveRegion(),
                user.getPreferredSport(),
                user.getProfileImg()
        );
    }
}