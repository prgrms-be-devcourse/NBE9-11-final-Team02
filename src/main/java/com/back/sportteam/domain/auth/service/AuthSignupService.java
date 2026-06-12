package com.back.sportteam.domain.auth.service;

import com.back.sportteam.domain.auth.dto.request.SignupRequest;
import com.back.sportteam.domain.auth.dto.response.SignupResponse;
import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.user.domain.User;
import com.back.sportteam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthSignupService {

    private final UserRepository userRepository;
    private final com.back.sportteam.auth.security.PasswordHasher passwordHasher;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateDuplicateEmail(request.email());
        AuthProvider provider = request.provider() == null ? AuthProvider.LOCAL : request.provider();
        User user = provider == AuthProvider.LOCAL
                ? createLocalUser(request)
                : createSocialUser(request, provider);
        return SignupResponse.from(userRepository.save(user));
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }
    }

    private User createLocalUser(SignupRequest request) {
        if (!StringUtils.hasText(request.password())) {
            throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_REQUEST, "일반 회원가입은 비밀번호가 필요합니다.");
        }
        return User.local(
                request.email(),
                request.nickname(),
                passwordHasher.hash(request.password()),
                request.role()
        );
    }

    private User createSocialUser(SignupRequest request, AuthProvider provider) {
        if (!StringUtils.hasText(request.providerId())) {
            throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_REQUEST, "소셜 회원가입은 providerId가 필요합니다.");
        }
        if (userRepository.findByProviderAndProviderId(provider, request.providerId()).isPresent()) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_SOCIAL_ACCOUNT);
        }
        return User.google(request.email(), request.nickname(), request.role(), request.providerId());
    }
}