package com.back.sportteam.domain.match.subscriber;

import com.back.sportteam.domain.match.dto.MatchStatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchStatusSubscriber implements MessageListener {

    private static final String TOPIC_PREFIX = "/topic/matches/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            MatchStatusMessage statusMessage = objectMapper.readValue(
                    message.getBody(), MatchStatusMessage.class
            );
            messagingTemplate.convertAndSend(
                    TOPIC_PREFIX + statusMessage.matchId(),
                    statusMessage
            );
        } catch (Exception e) {
            throw new IllegalStateException("모집 현황 메시지 역직렬화에 실패했습니다.", e);
        }
    }
}