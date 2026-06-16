package com.back.sportteam.domain.auth.service;

import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.JwtProvider;
import com.back.sportteam.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthLogoutService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final long accessTokenExpiry;

    public AuthLogoutService(
            JwtProvider jwtProvider,
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.access-token-validity-seconds}") long accessTokenExpiry
    ) {
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.accessTokenExpiry = accessTokenExpiry * 1000;
    }

    public void logout(String accessToken) {
        if (!jwtProvider.isValid(accessToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }

        Claims claims = jwtProvider.parse(accessToken);
        Long userId = claims.get("userId", Long.class);

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                "logout",
                accessTokenExpiry,
                TimeUnit.MILLISECONDS
        );
    }
}