package com.back.sportteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.auth.dto.response.TokenRefreshResponse;
import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.JwtProvider;
import com.back.sportteam.domain.auth.support.RefreshTokenCookieWriter;
import com.back.sportteam.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthRefreshServiceTest {

    private JwtProvider jwtProvider;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RefreshTokenCookieWriter refreshTokenCookieWriter;
    private HttpServletResponse httpResponse;
    private AuthRefreshService authRefreshService;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        refreshTokenCookieWriter = mock(RefreshTokenCookieWriter.class);
        httpResponse = mock(HttpServletResponse.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authRefreshService = new AuthRefreshService(
                jwtProvider,
                redisTemplate,
                refreshTokenCookieWriter,
                1209600L
        );
    }

    @DisplayName("리프레시 토큰으로 새 액세스 토큰 반환")
    @Test
    void 리프레시_토큰으로_새_액세스_토큰_반환() {
        String userId = UUID.randomUUID().toString();
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("USER");

        when(jwtProvider.isValid("valid-refresh-token")).thenReturn(true);
        when(jwtProvider.parse("valid-refresh-token")).thenReturn(claims);
        when(valueOperations.get("refresh:" + userId)).thenReturn("valid-refresh-token");
        when(jwtProvider.generateAccessToken(userId, "USER")).thenReturn("new-access-token");
        when(jwtProvider.generateRefreshToken(userId, "USER")).thenReturn("new-refresh-token");

        TokenRefreshResponse response = authRefreshService.refresh("valid-refresh-token", httpResponse);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
        verify(refreshTokenCookieWriter).add(httpResponse, "new-refresh-token", 1209600000L);
    }

    @DisplayName("유효하지 않은 토큰일 시 예외 발생")
    @Test
    void 유효하지_않은_토큰일_시_예외_발생() {
        when(jwtProvider.isValid("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authRefreshService.refresh("invalid-token", httpResponse))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN)
                );
    }

    @DisplayName("Redis에 토큰이 없을 시 예외 발생")
    @Test
    void Redis에_토큰이_없을_시_예외_발생() {
        String userId = UUID.randomUUID().toString();
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("USER");

        when(jwtProvider.isValid("valid-refresh-token")).thenReturn(true);
        when(jwtProvider.parse("valid-refresh-token")).thenReturn(claims);
        when(valueOperations.get("refresh:" + userId)).thenReturn(null);

        assertThatThrownBy(() -> authRefreshService.refresh("valid-refresh-token", httpResponse))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND)
                );
    }

    @DisplayName("저장된 토큰과 일치하지 않을 시 예외 발생 및 토큰 삭제")
    @Test
    void 저장된_토큰과_일치하지_않을_시_예외_발생_및_토큰_삭제() {
        String userId = UUID.randomUUID().toString();
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("role", String.class)).thenReturn("USER");

        when(jwtProvider.isValid("stolen-token")).thenReturn(true);
        when(jwtProvider.parse("stolen-token")).thenReturn(claims);
        when(valueOperations.get("refresh:" + userId)).thenReturn("original-refresh-token");

        assertThatThrownBy(() -> authRefreshService.refresh("stolen-token", httpResponse))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISMATCH)
                );
        verify(redisTemplate).delete("refresh:" + userId);
    }
}