package com.back.sportteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.auth.security.PasswordHasher;
import com.back.sportteam.domain.auth.dto.request.LoginRequest;
import com.back.sportteam.domain.auth.dto.response.LoginResponse;
import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.JwtProvider;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthLoginServiceTest {

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private JwtProvider jwtProvider;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private HttpServletResponse httpResponse;
    private AuthLoginService authLoginService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        jwtProvider = mock(JwtProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        httpResponse = mock(HttpServletResponse.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authLoginService = new AuthLoginService(
                userRepository,
                passwordHasher,
                jwtProvider,
                redisTemplate,
                1209600L,
                false,
                "Lax"
        );
    }

    @DisplayName("로그인 성공 시 액세스 토큰을 반환")
    @Test
    void 로그인_성공_시_액세스_토큰을_반환() {
        User user = User.local("dnclsehd122@gmail.com", "오상민", "hashed", UserRole.USER);
        LoginRequest request = new LoginRequest("dnclsehd122@gmail.com", "sangmin");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordHasher.verify(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtProvider.generateAccessToken(any(), anyString())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(any(), anyString())).thenReturn("refresh-token");

        LoginResponse response = authLoginService.login(request, httpResponse);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
        verify(httpResponse).addHeader(anyString(), anyString());
    }

    @DisplayName("존재하지 않는 이메일이면 예외 발생")
    @Test
    void 존재하지_않는_이메일이면_예외_발생() {
        LoginRequest request = new LoginRequest("none@example.com", "sangmin");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authLoginService.login(request, httpResponse))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
                );
    }

    @DisplayName("비밀번호가 일치하지 않으면 예외 발생")
    @Test
    void 비밀번호가_일치하지_않으면_예외_발생() {
        User user = User.local("dnclsehd122@gmail.com", "오상민", "hashed", UserRole.USER);
        LoginRequest request = new LoginRequest("dnclsehd122@gmail.com", "wrongpassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordHasher.verify(request.password(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authLoginService.login(request, httpResponse))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
                );
    }
}