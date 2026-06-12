package com.back.sportteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.auth.domain.AuthProvider;
import com.back.sportteam.domain.auth.dto.request.SignupRequest;
import com.back.sportteam.domain.auth.dto.response.SignupResponse;
import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.security.PasswordHasher;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.user.domain.User;
import com.back.sportteam.user.domain.UserRole;
import com.back.sportteam.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthSignupServiceTest {

    private UserRepository userRepository;
    private AuthSignupService authSignupService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authSignupService = new AuthSignupService(userRepository, new PasswordHasher());
    }

    @DisplayName("일반 회원가입")
    @Test
    void 일반_회원가입() {
        SignupRequest request = new SignupRequest(
                "dnclsehd122@gmail.com",
                "sangmin",
                "오상민",
                UserRole.USER,
                AuthProvider.LOCAL,
                null
        );
        givenAvailableEmail(request.email());
        givenSavedUserReturnsArgument();

        SignupResponse response = authSignupService.signup(request);

        assertThat(response.email()).isEqualTo("dnclsehd122@gmail.com");
        assertThat(response.nickname()).isEqualTo("오상민");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.provider()).isEqualTo(AuthProvider.LOCAL);
    }

    @DisplayName("소셜 회원가입")
    @Test
    void 소셜_회원가입() {
        SignupRequest request = new SignupRequest(
                "dnclsehd122@gmail.com",
                null,
                "소셜매니저상민",
                UserRole.MANAGER,
                AuthProvider.GOOGLE,
                "dnclsehd122"
        );
        givenAvailableEmail(request.email());
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "dnclsehd122"))
                .thenReturn(Optional.empty());
        givenSavedUserReturnsArgument();

        SignupResponse response = authSignupService.signup(request);

        assertThat(response.email()).isEqualTo("dnclsehd122@gmail.com");
        assertThat(response.role()).isEqualTo(UserRole.MANAGER);
        assertThat(response.provider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @DisplayName("이메일 중복 시 예외 발생")
    @Test
    void 이메일_중복_시_예외_발생() {
        SignupRequest request = new SignupRequest(
                "dnclsehd122@gmail.com",
                "sangmin",
                "중복상민",
                UserRole.USER,
                AuthProvider.LOCAL,
                null
        );
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authSignupService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATE_EMAIL)
                );
        verify(userRepository, never()).save(any(User.class));
    }

    @DisplayName("일반 회원가입에 비밀번호가 없을 시 예외 발생")
    @Test
    void 일반_회원가입에_비밀번호가_없을_시_예외_발생() {
        SignupRequest request = new SignupRequest(
                "dnclsehd122@gmail.com",
                null,
                "일반상민",
                UserRole.USER,
                AuthProvider.LOCAL,
                null
        );
        givenAvailableEmail(request.email());

        assertThatThrownBy(() -> authSignupService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_SIGNUP_REQUEST)
                );
        verify(userRepository, never()).save(any(User.class));
    }

    @DisplayName("소셜 회원가입에 providerId가 없을 시 예외 발생")
    @Test
    void 소셜_회원가입에_providerId가_없을_시_예외_발생() {
        SignupRequest request = new SignupRequest(
                "dnclsehd122@gmail.com",
                null,
                "소셜상민",
                UserRole.USER,
                AuthProvider.GOOGLE,
                null
        );
        givenAvailableEmail(request.email());

        assertThatThrownBy(() -> authSignupService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_SIGNUP_REQUEST)
                );
        verify(userRepository, never()).save(any(User.class));
    }

    @DisplayName("가입된 소셜 계정일 시 예외 발생")
    @Test
    void 가입된_소셜_계정일_시_예외_발생() {
        SignupRequest request = new SignupRequest(
                "dnclseh    d122@gmail.com",
                null,
                "소셜상민",
                UserRole.USER,
                AuthProvider.GOOGLE,
                "dnclsehd122"
        );
        givenAvailableEmail(request.email());
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "dnclsehd122"))
                .thenReturn(Optional.of(User.google(
                        "old@gmail.com",
                        "기존상민",
                        UserRole.USER,
                        "dnclsehd122"
                )));

        assertThatThrownBy(() -> authSignupService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.DUPLICATE_SOCIAL_ACCOUNT)
                );
        verify(userRepository, never()).save(any(User.class));
    }

    private void givenAvailableEmail(String email) {
        when(userRepository.existsByEmail(email)).thenReturn(false);
    }

    private void givenSavedUserReturnsArgument() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}