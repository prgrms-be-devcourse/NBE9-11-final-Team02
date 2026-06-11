package com.back.sportteam.global.response;

public record ErrorResponse(
        String code,
        String message,
        int status,
        String path
) {
}
