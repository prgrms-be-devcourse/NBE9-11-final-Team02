package com.back.sportteam.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.user.dto.request.UserProfileUpdateRequest;
import com.back.sportteam.domain.user.dto.response.UserProfileUpdateResponse;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfileUpdateServiceTest {

    private UserRepository userRepository;
    private UserProfileUpdateService userProfileUpdateService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userProfileUpdateService = new UserProfileUpdateService(userRepository);
    }

    @DisplayName("내 프로필 수정 성공")
    @Test
    void 내_프로필_수정_성공() {
        UUID userId = UUID.randomUUID();
        User user = User.local("dnclsehd122@gmail.com", "오상민", "hashed", UserRole.USER);
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "상민아",
                "FW",
                "창원",
                "풋살",
                "https://live.lge.co.kr/wp-content/uploads/2013/03/Penguins1.jpg"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileUpdateResponse response = userProfileUpdateService.updateMyProfile(userId, request);

        assertThat(response.nickname()).isEqualTo("상민아");
        assertThat(response.position()).isEqualTo("FW");
        assertThat(response.activeRegion()).isEqualTo("창원");
        assertThat(response.preferredSport()).isEqualTo("풋살");
        assertThat(response.profileImg()).isEqualTo("https://live.lge.co.kr/wp-content/uploads/2013/03/Penguins1.jpg");
    }

    @DisplayName("null 필드는 기존 값을 유지")
    @Test
    void null_필드는_기존_값을_유지() {
        UUID userId = UUID.randomUUID();
        User user = User.local("dnclsehd122@gmail.com", "오상민", "hashed", UserRole.USER);
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "상민아",
                null,
                null,
                null,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileUpdateResponse response = userProfileUpdateService.updateMyProfile(userId, request);

        assertThat(response.nickname()).isEqualTo("상민아");
        assertThat(response.position()).isNull();
        assertThat(response.activeRegion()).isNull();
    }

    @DisplayName("존재하지 않는 유저일 시 예외 발생")
    @Test
    void 존재하지_않는_유저일_시_예외_발생() {
        UUID userId = UUID.randomUUID();
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "상민아", null, null, null, null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileUpdateService.updateMyProfile(userId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
                );
    }
}