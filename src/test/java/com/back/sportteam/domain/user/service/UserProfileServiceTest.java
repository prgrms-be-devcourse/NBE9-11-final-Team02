package com.back.sportteam.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.dto.response.UserProfileResponse;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileServiceTest {

    private UserRepository userRepository;
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userProfileService = new UserProfileService(userRepository);
    }

    @DisplayName("내 프로필 조회 성공")
    @Test
    void 내_프로필_조회_성공() {
        String userId = UUID.randomUUID().toString();
        User user = User.local("dnclsehd122@gmail.com", "오상민", "hashed", UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getMyProfile(userId);

        assertThat(response.email()).isEqualTo("dnclsehd122@gmail.com");
        assertThat(response.nickname()).isEqualTo("오상민");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.provider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(response.mannerScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.skillScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @DisplayName("존재하지 않는 유저일 시 예외 발생")
    @Test
    void 존재하지_않는_유저일_시_예외_발생() {
        String userId = UUID.randomUUID().toString();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getMyProfile(userId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
                );
    }
}