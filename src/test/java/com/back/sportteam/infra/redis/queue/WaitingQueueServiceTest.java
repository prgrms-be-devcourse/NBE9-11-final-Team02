package com.back.sportteam.infra.redis.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.system.dto.response.WaitingQueueTokenResponse;
import com.back.sportteam.domain.system.exception.SystemErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WaitingQueueServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private FacilitySlotRepository facilitySlotRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private WaitingQueueService waitingQueueService;

    @BeforeEach
    void setUp() {
        waitingQueueService = new WaitingQueueService(redisTemplate, facilitySlotRepository);
        ReflectionTestUtils.setField(waitingQueueService, "tokenTtlSeconds", 300L);
        ReflectionTestUtils.setField(waitingQueueService, "entryLimit", 1L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void issueTokenAddsTokenToSlotQueue() {
        stubFacilitySlotExists("slot-id");
        when(valueOperations.get(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (key.equals(WaitingQueueKeys.userToken("slot-id", "user-id"))) {
                return null;
            }
            return "slot-id";
        });
        when(zSetOperations.range(WaitingQueueKeys.queue("slot-id"), 0, -1)).thenReturn(Set.of());
        when(zSetOperations.add(eq(WaitingQueueKeys.queue("slot-id")), any(String.class), anyDouble()))
                .thenReturn(true);
        when(zSetOperations.rank(eq(WaitingQueueKeys.queue("slot-id")), any(String.class))).thenReturn(0L);
        when(zSetOperations.size(WaitingQueueKeys.queue("slot-id"))).thenReturn(1L);

        WaitingQueueTokenResponse response = waitingQueueService.issueToken("slot-id", "user-id");

        assertThat(response.facilitySlotId()).isEqualTo("slot-id");
        assertThat(response.position()).isEqualTo(1);
        assertThat(response.waitingCount()).isZero();
        assertThat(response.enterable()).isTrue();
        verify(valueOperations).set(any(String.class), eq("slot-id"), eq(Duration.ofSeconds(300)));
        verify(valueOperations).set(eq(WaitingQueueKeys.userToken("slot-id", "user-id")),
                any(String.class), eq(Duration.ofSeconds(300)));
        verify(zSetOperations).add(eq(WaitingQueueKeys.queue("slot-id")), any(String.class), anyDouble());
    }

    @Test
    void issueTokenReusesExistingTokenForSameUserAndSlot() {
        stubFacilitySlotExists("slot-id");
        when(valueOperations.get(WaitingQueueKeys.userToken("slot-id", "user-id"))).thenReturn("existing-token");
        when(valueOperations.get(WaitingQueueKeys.token("existing-token"))).thenReturn("slot-id");
        when(zSetOperations.range(WaitingQueueKeys.queue("slot-id"), 0, -1)).thenReturn(Set.of("existing-token"));
        when(zSetOperations.rank(WaitingQueueKeys.queue("slot-id"), "existing-token")).thenReturn(0L);
        when(zSetOperations.size(WaitingQueueKeys.queue("slot-id"))).thenReturn(1L);

        WaitingQueueTokenResponse response = waitingQueueService.issueToken("slot-id", "user-id");

        assertThat(response.token()).isEqualTo("existing-token");
        assertThat(response.enterable()).isTrue();
        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
        verify(zSetOperations, never()).add(any(String.class), any(String.class), anyDouble());
    }

    @Test
    void issueTokenRemovesExpiredTokenFromSlotQueue() {
        stubFacilitySlotExists("slot-id");
        when(zSetOperations.range(WaitingQueueKeys.queue("slot-id"), 0, -1))
                .thenReturn(Set.of("expired-token"));
        when(valueOperations.get(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (key.equals(WaitingQueueKeys.userToken("slot-id", "user-id"))
                    || key.equals(WaitingQueueKeys.token("expired-token"))) {
                return null;
            }
            return "slot-id";
        });
        when(zSetOperations.rank(eq(WaitingQueueKeys.queue("slot-id")), any(String.class))).thenReturn(0L);
        when(zSetOperations.size(WaitingQueueKeys.queue("slot-id"))).thenReturn(1L);

        waitingQueueService.issueToken("slot-id", "user-id");

        verify(zSetOperations, atLeastOnce()).remove(WaitingQueueKeys.queue("slot-id"), "expired-token");
    }

    @Test
    void issueTokenThrowsWhenFacilitySlotDoesNotExist() {
        when(facilitySlotRepository.findById("missing-slot")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingQueueService.issueToken("missing-slot", "user-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND);
    }

    @Test
    void getStatusReturnsQueuePosition() {
        String token = "token-id";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn("slot-id");
        when(zSetOperations.range(WaitingQueueKeys.queue("slot-id"), 0, -1)).thenReturn(Set.of(token));
        when(zSetOperations.rank(WaitingQueueKeys.queue("slot-id"), token)).thenReturn(2L);
        when(zSetOperations.size(WaitingQueueKeys.queue("slot-id"))).thenReturn(5L);

        WaitingQueueTokenResponse response = waitingQueueService.getStatus(token);

        assertThat(response.position()).isEqualTo(3);
        assertThat(response.waitingCount()).isEqualTo(4);
        assertThat(response.enterable()).isFalse();
    }

    @Test
    void getStatusThrowsWhenTokenExpired() {
        when(valueOperations.get(WaitingQueueKeys.token("expired-token"))).thenReturn(null);

        assertThatThrownBy(() -> waitingQueueService.getStatus("expired-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SystemErrorCode.QUEUE_TOKEN_EXPIRED);
    }

    @Test
    void getStatusThrowsWhenTokenIsNotInQueue() {
        String token = "token-id";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn("slot-id");
        when(zSetOperations.rank(WaitingQueueKeys.queue("slot-id"), token)).thenReturn(null);

        assertThatThrownBy(() -> waitingQueueService.getStatus(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SystemErrorCode.QUEUE_TOKEN_INVALID);
    }

    @Test
    void getStatusReturnsZeroWaitingCountWhenQueueSizeIsNull() {
        String token = "token-id";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn("slot-id");
        when(zSetOperations.range(WaitingQueueKeys.queue("slot-id"), 0, -1)).thenReturn(Set.of(token));
        when(zSetOperations.rank(WaitingQueueKeys.queue("slot-id"), token)).thenReturn(0L);
        when(zSetOperations.size(WaitingQueueKeys.queue("slot-id"))).thenReturn(null);

        WaitingQueueTokenResponse response = waitingQueueService.getStatus(token);

        assertThat(response.position()).isEqualTo(1);
        assertThat(response.waitingCount()).isZero();
        assertThat(response.enterable()).isTrue();
    }

    @Test
    void consumeEnterableTokenRemovesTokenAndUserToken() {
        String token = "token-id";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn("slot-id");
        when(valueOperations.get(WaitingQueueKeys.userToken("slot-id", "user-id"))).thenReturn(token);
        when(zSetOperations.rank(WaitingQueueKeys.queue("slot-id"), token)).thenReturn(0L);

        waitingQueueService.consumeEnterableToken(token, "user-id");

        verify(zSetOperations).remove(WaitingQueueKeys.queue("slot-id"), token);
        verify(redisTemplate).delete(WaitingQueueKeys.token(token));
        verify(redisTemplate).delete(WaitingQueueKeys.userToken("slot-id", "user-id"));
    }

    @Test
    void consumeEnterableTokenThrowsWhenTokenIsBlank() {
        assertThatThrownBy(() -> waitingQueueService.consumeEnterableToken(" ", "slot-id", "user-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SystemErrorCode.QUEUE_TOKEN_REQUIRED);
    }

    @Test
    void consumeEnterableTokenThrowsWhenTokenExpired() {
        String token = "expired-token";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn(null);

        assertThatThrownBy(() -> waitingQueueService.consumeEnterableToken(token, "slot-id", "user-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SystemErrorCode.QUEUE_TOKEN_EXPIRED);
    }

    @Test
    void consumeEnterableTokenThrowsWhenTokenBelongsToDifferentSlot() {
        String token = "token-id";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn("other-slot-id");
        when(valueOperations.get(WaitingQueueKeys.userToken("slot-id", "user-id"))).thenReturn(token);

        assertThatThrownBy(() -> waitingQueueService.consumeEnterableToken(token, "slot-id", "user-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SystemErrorCode.QUEUE_TOKEN_INVALID);
    }

    @Test
    void consumeEnterableTokenThrowsWhenTokenIsNotInQueue() {
        String token = "token-id";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn("slot-id");
        when(valueOperations.get(WaitingQueueKeys.userToken("slot-id", "user-id"))).thenReturn(token);
        when(zSetOperations.rank(WaitingQueueKeys.queue("slot-id"), token)).thenReturn(null);

        assertThatThrownBy(() -> waitingQueueService.consumeEnterableToken(token, "slot-id", "user-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SystemErrorCode.QUEUE_TOKEN_INVALID);
    }

    @Test
    void consumeEnterableTokenThrowsWhenTokenIsNotEnterableYet() {
        String token = "token-id";
        when(valueOperations.get(WaitingQueueKeys.token(token))).thenReturn("slot-id");
        when(valueOperations.get(WaitingQueueKeys.userToken("slot-id", "user-id"))).thenReturn(token);
        when(zSetOperations.rank(WaitingQueueKeys.queue("slot-id"), token)).thenReturn(1L);

        assertThatThrownBy(() -> waitingQueueService.consumeEnterableToken(token, "slot-id", "user-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SystemErrorCode.QUEUE_NOT_ENTERABLE);

        verify(zSetOperations, never()).remove(WaitingQueueKeys.queue("slot-id"), token);
        verify(redisTemplate, never()).delete(WaitingQueueKeys.token(token));
    }

    private void stubFacilitySlotExists(String facilitySlotId) {
        FacilitySlot facilitySlot = facilitySlot(facilitySlotId);
        when(facilitySlotRepository.findById(facilitySlotId)).thenReturn(Optional.of(facilitySlot));
    }

    private FacilitySlot facilitySlot(String facilitySlotId) {
        FacilitySlot facilitySlot = org.mockito.Mockito.mock(FacilitySlot.class);
        when(facilitySlot.getId()).thenReturn(facilitySlotId);
        return facilitySlot;
    }
}
