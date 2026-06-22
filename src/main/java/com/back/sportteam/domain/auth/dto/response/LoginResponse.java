package com.back.sportteam.domain.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String role
) {
    public static LoginResponse of(String accessToken, String role) {
        return new LoginResponse(accessToken, role);
    }
}