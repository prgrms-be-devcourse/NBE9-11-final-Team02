package com.back.sportteam.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class UserWithdrawServiceTest {

    private UserRepository userRepository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private UserWithdrawService userWithdrawService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        userWithdrawService = new UserWithdrawService(
                userRepository,
                redisTemplate,
                1800L
        );
    }

    @DisplayName("회원 탈퇴 성공 시 RefreshToken 삭제 및 AccessToken 블랙리스트 등록 후 유저 삭제")
    @Test
    void 회원_탈퇴_성공_시_RefreshToken_삭제_및_AccessToken_블랙리스트_등록_후_유저_삭제() {
        String userId = UUID.randomUUID().toString();
        User user = User.local("dnclsehd122@gmail.com", "오상민", "hashed", UserRole.USER);

        when(userRepository.findById(String.valueOf(userId))).thenReturn(Optional.of(user));

        userWithdrawService.withdraw(userId, "valid-access-token");

        verify(redisTemplate).delete("refresh:" + userId);
        verify(valueOperations).set(
                eq("blacklist:valid-access-token"),
                eq("withdrawn"),
                anyLong(),
                any()
        );
        verify(userRepository).delete(user);
    }

    @DisplayName("존재하지 않는 유저일 시 예외 발생")
    @Test
    void 존재하지_않는_유저일_시_예외_발생() {
        String userId = UUID.randomUUID().toString();

        when(userRepository.findById(String.valueOf(userId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userWithdrawService.withdraw(userId, "valid-access-token"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
                );
    }
}