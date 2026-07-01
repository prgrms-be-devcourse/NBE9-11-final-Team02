package com.back.sportteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.JwtProvider;
import com.back.sportteam.domain.auth.support.RefreshTokenCookieWriter;
import com.back.sportteam.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthLogoutServiceTest {

    private JwtProvider jwtProvider;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RefreshTokenCookieWriter refreshTokenCookieWriter;
    private HttpServletResponse httpResponse;
    private AuthLogoutService authLogoutService;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        refreshTokenCookieWriter = mock(RefreshTokenCookieWriter.class);
        httpResponse = mock(HttpServletResponse.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authLogoutService = new AuthLogoutService(
                jwtProvider,
                redisTemplate,
                refreshTokenCookieWriter,
                1800L
        );
    }

    @DisplayName("로그아웃 성공 시 RefreshToken 삭제 및 AccessToken 블랙리스트 등록")
    @Test
    void 로그아웃_성공_시_RefreshToken_삭제_및_AccessToken_블랙리스트_등록() {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("user-id");

        when(jwtProvider.isValid("valid-access-token")).thenReturn(true);
        when(jwtProvider.parse("valid-access-token")).thenReturn(claims);

        authLogoutService.logout("valid-access-token", httpResponse);

        verify(redisTemplate).delete("refresh:user-id");
        verify(valueOperations).set(
                eq("blacklist:valid-access-token"),
                eq("logout"),
                anyLong(),
                any()
        );
        verify(refreshTokenCookieWriter).clear(httpResponse);
    }

    @DisplayName("유효하지 않은 토큰일 시 예외 발생")
    @Test
    void 유효하지_않은_토큰일_시_예외_발생() {
        when(jwtProvider.isValid("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authLogoutService.logout("invalid-token", httpResponse))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN)
                );
        verify(redisTemplate, never()).delete(anyString());
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }
}
