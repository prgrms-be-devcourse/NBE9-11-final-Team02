package com.back.sportteam.infra.redis.queue;

import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.system.dto.response.WaitingQueueTokenResponse;
import com.back.sportteam.domain.system.exception.SystemErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.util.TimeUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private static final String DELIMITER = ":";

    private final StringRedisTemplate redisTemplate;
    private final FacilitySlotRepository facilitySlotRepository;

    @Value("${app.queue.reservation.token-ttl-seconds:300}")
    private long tokenTtlSeconds;

    @Value("${app.queue.reservation.entry-limit:1}")
    private long entryLimit;

    public WaitingQueueTokenResponse issueToken(String facilitySlotId, String userId) {
        if (!facilitySlotRepository.existsById(facilitySlotId)) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND);
        }

        cleanupExpiredTokens(WaitingQueueKeys.queue(facilitySlotId));
        WaitingQueueTokenResponse existingToken = findExistingToken(facilitySlotId, userId);
        if (existingToken != null) {
            return existingToken;
        }

        String token = UUID.randomUUID().toString();
        String queueKey = WaitingQueueKeys.queue(facilitySlotId);
        String tokenKey = WaitingQueueKeys.token(token);
        String userTokenKey = WaitingQueueKeys.userToken(facilitySlotId, userId);
        long issuedAt = System.currentTimeMillis();

        redisTemplate.opsForValue().set(
                tokenKey,
                facilitySlotId + DELIMITER + hashUserId(userId),
                Duration.ofSeconds(tokenTtlSeconds)
        );
        redisTemplate.opsForValue().set(
                userTokenKey,
                token,
                Duration.ofSeconds(tokenTtlSeconds)
        );
        redisTemplate.opsForZSet().add(queueKey, token, issuedAt);

        return getStatus(token);
    }

    private WaitingQueueTokenResponse findExistingToken(String facilitySlotId, String userId) {
        String userTokenKey = WaitingQueueKeys.userToken(facilitySlotId, userId);
        String existingToken = redisTemplate.opsForValue().get(userTokenKey);
        if (existingToken == null || existingToken.isBlank()) {
            return null;
        }

        try {
            return getStatus(existingToken);
        } catch (BusinessException e) {
            if (e.getErrorCode() != SystemErrorCode.QUEUE_TOKEN_EXPIRED
                    && e.getErrorCode() != SystemErrorCode.QUEUE_TOKEN_INVALID) {
                throw e;
            }
            redisTemplate.delete(userTokenKey);
            return null;
        }
    }

    public WaitingQueueTokenResponse getStatus(String token) {
        String tokenKey = WaitingQueueKeys.token(token);
        String tokenValue = redisTemplate.opsForValue().get(tokenKey);
        if (tokenValue == null) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_EXPIRED);
        }

        TokenPayload payload = parseTokenPayload(tokenValue);
        String queueKey = WaitingQueueKeys.queue(payload.facilitySlotId());
        cleanupExpiredTokens(queueKey);
        Long rank = redisTemplate.opsForZSet().rank(queueKey, token);
        if (rank == null) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_INVALID);
        }

        long position = rank + 1;
        long waitingCount = getWaitingCount(queueKey);

        return new WaitingQueueTokenResponse(
                token,
                payload.facilitySlotId(),
                position,
                waitingCount,
                position <= entryLimit,
                calculateExpiresAt(tokenKey)
        );
    }

    public void consumeEnterableToken(String token, String facilitySlotId, String userId) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_REQUIRED);
        }

        String tokenKey = WaitingQueueKeys.token(token);
        String tokenValue = redisTemplate.opsForValue().get(tokenKey);
        if (tokenValue == null) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_EXPIRED);
        }

        TokenPayload payload = parseTokenPayload(tokenValue);
        if (!payload.facilitySlotId().equals(facilitySlotId) || !payload.userHash().equals(hashUserId(userId))) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_INVALID);
        }

        String queueKey = WaitingQueueKeys.queue(facilitySlotId);
        Long rank = redisTemplate.opsForZSet().rank(queueKey, token);
        if (rank == null) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_INVALID);
        }
        if (rank + 1 > entryLimit) {
            throw new BusinessException(SystemErrorCode.QUEUE_NOT_ENTERABLE);
        }

        redisTemplate.opsForZSet().remove(queueKey, token);
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(WaitingQueueKeys.userToken(facilitySlotId, userId));
    }

    private void cleanupExpiredTokens(String queueKey) {
        Set<String> tokens = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String token : tokens) {
            if (redisTemplate.opsForValue().get(WaitingQueueKeys.token(token)) == null) {
                redisTemplate.opsForZSet().remove(queueKey, token);
            }
        }
    }

    private TokenPayload parseTokenPayload(String tokenValue) {
        int delimiterIndex = tokenValue.indexOf(DELIMITER);
        if (delimiterIndex <= 0) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_INVALID);
        }
        String userHash = tokenValue.substring(delimiterIndex + 1);
        if (userHash.isBlank()) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_INVALID);
        }
        return new TokenPayload(tokenValue.substring(0, delimiterIndex), userHash);
    }

    private String hashUserId(String userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable.", e);
        }
    }

    private LocalDateTime calculateExpiresAt(String tokenKey) {
        Long remainingSeconds = redisTemplate.getExpire(tokenKey);
        if (remainingSeconds == null || remainingSeconds < 0) {
            remainingSeconds = tokenTtlSeconds;
        }
        return LocalDateTime.now(TimeUtils.SERVICE_ZONE).plusSeconds(remainingSeconds);
    }

    private long getWaitingCount(String queueKey) {
        Long size = redisTemplate.opsForZSet().size(queueKey);
        if (size == null) {
            return 0;
        }
        return Math.max(size - entryLimit, 0);
    }

    private record TokenPayload(
            String facilitySlotId,
            String userHash
    ) {
    }
}
