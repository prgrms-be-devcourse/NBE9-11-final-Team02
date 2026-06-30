package com.back.sportteam.domain.auth.service;

import com.back.sportteam.domain.auth.dto.request.LoginRequest;
import com.back.sportteam.domain.auth.dto.response.LoginResponse;
import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.JwtProvider;
import com.back.sportteam.domain.auth.security.PasswordHasher;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthLoginService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final long refreshTokenExpiry;
    private final boolean secureCookie;
    private final String sameSite;

    public AuthLoginService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            JwtProvider jwtProvider,
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.refresh-token-validity-seconds}") long refreshTokenExpiry,
            @Value("${app.jwt.secure-cookie}") boolean secureCookie,
            @Value("${app.jwt.same-site}") String sameSite
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiry = refreshTokenExpiry * 1000;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordHasher.verify(request.password(), user.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole().name());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                refreshTokenExpiry,
                TimeUnit.MILLISECONDS
        );

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/refresh")
                .maxAge(refreshTokenExpiry / 1000)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return LoginResponse.of(accessToken, user.getRole().name());
    }
}