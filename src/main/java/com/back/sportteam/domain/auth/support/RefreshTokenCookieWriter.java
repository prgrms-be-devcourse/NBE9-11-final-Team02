package com.back.sportteam.domain.auth.support;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieWriter {

    private final String cookieName;
    private final String cookiePath;
    private final boolean secureCookie;
    private final String sameSite;

    public RefreshTokenCookieWriter(
            @Value("${app.jwt.refresh-token-cookie-name}") String cookieName,
            @Value("${app.jwt.refresh-token-cookie-path}") String cookiePath,
            @Value("${app.jwt.secure-cookie}") boolean secureCookie,
            @Value("${app.jwt.same-site}") String sameSite
    ) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public void add(HttpServletResponse response, String refreshToken, long maxAgeMillis) {
        ResponseCookie cookie = baseCookie(refreshToken)
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath);
    }
}
