package com.back.sportteam.domain.match.publisher;

import com.back.sportteam.domain.match.dto.MatchStatusMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchStatusPublisher {

    private static final String CHANNEL_PREFIX = "match:status:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(MatchStatusMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL_PREFIX + message.matchId(), payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("모집 현황 메시지 직렬화에 실패했습니다.", e);
        }
    }
}