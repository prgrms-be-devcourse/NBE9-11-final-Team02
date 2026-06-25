package com.back.sportteam.domain.admin.dto.response;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminUserResponse(
        String userId,
        String email,
        String nickname,
        UserRole role,
        AuthProvider provider,
        String activeRegion,
        BigDecimal mannerScore,
        BigDecimal skillScore,
        int mannerReviewCount,
        boolean restricted,
        String restrictionReason,
        LocalDateTime restrictedAt,
        LocalDateTime createdAt
) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getActiveRegion(),
                user.getMannerScore(),
                user.getSkillScore(),
                user.getMannerReviewCount(),
                user.isRestricted(),
                user.getRestrictionReason(),
                user.getRestrictedAt(),
                user.getCreatedAt()
        );
    }
}
