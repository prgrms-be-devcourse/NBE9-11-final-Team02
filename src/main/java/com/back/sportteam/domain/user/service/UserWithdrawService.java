package com.back.sportteam.domain.user.service;

import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWithdrawService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final long accessTokenExpiry;

    public UserWithdrawService(
            UserRepository userRepository,
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.access-token-validity-seconds}") long accessTokenExpiry
    ) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.accessTokenExpiry = accessTokenExpiry * 1000;
    }

    @Transactional
    public void withdraw(UUID userId, String accessToken) {
        User user = userRepository.findById(String.valueOf(userId))
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
        userRepository.flush();

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                "withdrawn",
                accessTokenExpiry,
                TimeUnit.MILLISECONDS
        );
    }
}