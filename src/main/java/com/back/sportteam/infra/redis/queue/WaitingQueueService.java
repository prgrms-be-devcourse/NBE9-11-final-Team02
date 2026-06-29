package com.back.sportteam.infra.redis.queue;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.system.dto.response.WaitingQueueTokenResponse;
import com.back.sportteam.domain.system.exception.SystemErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.util.TimeUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private final StringRedisTemplate redisTemplate;
    private final FacilitySlotRepository facilitySlotRepository;

    @Value("${app.queue.reservation.token-ttl-seconds:300}")
    private long tokenTtlSeconds;

    @Value("${app.queue.reservation.entry-limit:1}")
    private long entryLimit;

    public WaitingQueueTokenResponse issueToken(String facilitySlotId, String userId) {
        String verifiedFacilitySlotId = getVerifiedFacilitySlotId(facilitySlotId);

        cleanupExpiredTokens(WaitingQueueKeys.queue(verifiedFacilitySlotId));
        WaitingQueueTokenResponse existingToken = findExistingToken(verifiedFacilitySlotId, userId);
        if (existingToken != null) {
            return existingToken;
        }

        String token = UUID.randomUUID().toString();
        String queueKey = WaitingQueueKeys.queue(verifiedFacilitySlotId);
        String tokenKey = WaitingQueueKeys.token(token);
        String userTokenKey = WaitingQueueKeys.userToken(verifiedFacilitySlotId, userId);
        long issuedAt = System.currentTimeMillis();

        redisTemplate.opsForValue().set(
                tokenKey,
                verifiedFacilitySlotId,
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

    private String getVerifiedFacilitySlotId(String facilitySlotId) {
        FacilitySlot facilitySlot = facilitySlotRepository.findById(facilitySlotId)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND));
        return facilitySlot.getId();
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

        String facilitySlotId = parseFacilitySlotId(tokenValue);
        String queueKey = WaitingQueueKeys.queue(facilitySlotId);
        cleanupExpiredTokens(queueKey);
        Long rank = redisTemplate.opsForZSet().rank(queueKey, token);
        if (rank == null) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_INVALID);
        }

        long position = rank + 1;
        long waitingCount = getWaitingCount(queueKey);

        return new WaitingQueueTokenResponse(
                token,
                facilitySlotId,
                position,
                waitingCount,
                position <= entryLimit,
                calculateExpiresAt(tokenKey)
        );
    }

    public void consumeEnterableToken(String token, String userId) {
        String facilitySlotId = getFacilitySlotIdFromToken(token);
        consumeEnterableToken(token, facilitySlotId, userId);
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

        String tokenFacilitySlotId = parseFacilitySlotId(tokenValue);
        String issuedToken = redisTemplate.opsForValue().get(WaitingQueueKeys.userToken(facilitySlotId, userId));
        if (!tokenFacilitySlotId.equals(facilitySlotId) || !token.equals(issuedToken)) {
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

    private String getFacilitySlotIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_REQUIRED);
        }

        String tokenValue = redisTemplate.opsForValue().get(WaitingQueueKeys.token(token));
        if (tokenValue == null) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_EXPIRED);
        }
        return parseFacilitySlotId(tokenValue);
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

    private String parseFacilitySlotId(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new BusinessException(SystemErrorCode.QUEUE_TOKEN_INVALID);
        }
        return tokenValue;
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

}
