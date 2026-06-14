package com.back.sportteam.domain.auth.service;

import com.back.sportteam.auth.security.PasswordHasher;
import com.back.sportteam.domain.auth.dto.request.LoginRequest;
import com.back.sportteam.domain.auth.dto.response.LoginResponse;
import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.JwtProvider;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.user.domain.User;
import com.back.sportteam.user.repository.UserRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthLoginService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    public LoginResponse login(LoginRequest request, jakarta.servlet.http.HttpServletResponse response) {
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

        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiry / 1000));
        response.addCookie(cookie);

        return LoginResponse.of(accessToken);
    }
}