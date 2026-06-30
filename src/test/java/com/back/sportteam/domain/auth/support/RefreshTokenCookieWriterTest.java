package com.back.sportteam.domain.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;

class RefreshTokenCookieWriterTest {

    @DisplayName("refresh token cookie includes SameSite and limited Path")
    @Test
    void refreshTokenCookieIncludesSameSiteAndLimitedPath() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        RefreshTokenCookieWriter writer = new RefreshTokenCookieWriter(
                "refreshToken",
                "/api/v1/auth/refresh",
                false,
                "Lax"
        );
        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);

        writer.add(response, "refresh-token", 1209600000L);

        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        assertThat(cookieCaptor.getValue())
                .contains(
                        "refreshToken=refresh-token",
                        "Path=/api/v1/auth/refresh",
                        "Max-Age=1209600",
                        "HttpOnly",
                        "SameSite=Lax"
                );
    }

    @DisplayName("cleared refresh token cookie uses same Path")
    @Test
    void clearedRefreshTokenCookieUsesSamePath() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        RefreshTokenCookieWriter writer = new RefreshTokenCookieWriter(
                "refreshToken",
                "/api/v1/auth/refresh",
                false,
                "Lax"
        );
        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);

        writer.clear(response);

        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        assertThat(cookieCaptor.getValue())
                .contains(
                        "refreshToken=",
                        "Path=/api/v1/auth/refresh",
                        "Max-Age=0",
                        "HttpOnly",
                        "SameSite=Lax"
                );
    }
}
