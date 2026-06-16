package com.back.sportteam.domain.auth.dto.response;

public record TokenRefreshResponse(
        String accessToken
) {
    public static TokenRefreshResponse of(String accessToken) {
        return new TokenRefreshResponse(accessToken);
    }
}