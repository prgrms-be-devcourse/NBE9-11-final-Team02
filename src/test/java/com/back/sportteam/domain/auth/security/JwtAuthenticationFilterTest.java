package com.back.sportteam.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtProvider jwtProvider;
    private StringRedisTemplate redisTemplate;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        filter = new JwtAuthenticationFilter(jwtProvider, redisTemplate);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("유효한 토큰일 시 SecurityContext에 인증 정보를 등록")
    @Test
    void 유효한_토큰일_시_SecurityContext에_인증_정보를_등록() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(claims.get("role", String.class)).thenReturn("USER");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtProvider.isValid("valid-token")).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:valid-token")).thenReturn(false);
        when(jwtProvider.parse("valid-token")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(getContext().getAuthentication()).isNotNull();
        assertThat(getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("토큰이 없으면 SecurityContext에 인증 정보를 등록하지 않음")
    @Test
    void 토큰이_없으면_SecurityContext에_인증_정보를_등록하지_않음() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @DisplayName("유효하지 않은 토큰일 시 예외 발생")
    @Test
    void 유효하지_않은_토큰일_시_예외_발생() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtProvider.isValid("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN)
                );
        verify(filterChain, never()).doFilter(request, response);
    }

    @DisplayName("블랙리스트에 등록된 토큰일 시 예외 발생")
    @Test
    void 블랙리스트에_등록된_토큰일_시_예외_발생() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer blacklisted-token");
        when(jwtProvider.isValid("blacklisted-token")).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:blacklisted-token")).thenReturn(true);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.ALREADY_LOGGED_OUT)
                );
        verify(filterChain, never()).doFilter(request, response);
    }
}