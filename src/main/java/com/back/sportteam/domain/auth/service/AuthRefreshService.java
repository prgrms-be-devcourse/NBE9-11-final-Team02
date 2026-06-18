package com.back.sportteam.domain.auth.service;

import com.back.sportteam.domain.auth.dto.response.TokenRefreshResponse;
import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.JwtProvider;
import com.back.sportteam.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthRefreshService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenExpiry;
    private final boolean secureCookie;

    public AuthRefreshService(
            JwtProvider jwtProvider,
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.refresh-token-validity-seconds}") long refreshTokenExpiry,
            @Value("${app.jwt.secure-cookie}") boolean secureCookie
    ) {
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiry = refreshTokenExpiry * 1000;
        this.secureCookie = secureCookie;
    }

    public TokenRefreshResponse refresh(String refreshToken, HttpServletResponse response) {
        if (!jwtProvider.isValid(refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }

        Claims claims = jwtProvider.parse(refreshToken);
        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);

        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!storedToken.equals(refreshToken)) {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId, role);

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                refreshTokenExpiry,
                TimeUnit.MILLISECONDS
        );

        Cookie cookie = new Cookie("refreshToken", newRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiry / 1000));
        response.addCookie(cookie);

        return TokenRefreshResponse.of(newAccessToken);
    }
}