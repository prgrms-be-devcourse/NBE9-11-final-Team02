package com.back.sportteam.domain.match.publisher;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.back.sportteam.domain.match.dto.MatchStatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class MatchStatusPublisherTest {

    private StringRedisTemplate redisTemplate;
    private MatchStatusPublisher publisher;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        publisher = new MatchStatusPublisher(redisTemplate, new ObjectMapper());
    }

    @DisplayName("모집 현황 메시지를 Redis 채널에 발행")
    @Test
    void 모집_현황_메시지를_Redis_채널에_발행() {
        MatchStatusMessage message = new MatchStatusMessage("50", 5, 10);

        publisher.publish(message);

        verify(redisTemplate).convertAndSend(eq("match:status:50"), anyString());
    }
}