package com.back.sportteam.domain.match.subscriber;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.back.sportteam.domain.match.dto.MatchStatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MatchStatusSubscriberTest {

    private SimpMessagingTemplate messagingTemplate;
    private MatchStatusSubscriber subscriber;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        subscriber = new MatchStatusSubscriber(messagingTemplate, new ObjectMapper());
    }

    @DisplayName("Redis 메시지 수신 시 STOMP 토픽으로 브로드캐스트")
    @Test
    void Redis_메시지_수신_시_STOMP_토픽으로_브로드캐스트() throws Exception {
        MatchStatusMessage message = new MatchStatusMessage("50", 5, 10);
        byte[] payload = new ObjectMapper().writeValueAsBytes(message);
        DefaultMessage redisMessage = new DefaultMessage("match:status:50".getBytes(), payload);

        subscriber.onMessage(redisMessage, null);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/matches/50"),
                any(MatchStatusMessage.class)
        );
    }
}