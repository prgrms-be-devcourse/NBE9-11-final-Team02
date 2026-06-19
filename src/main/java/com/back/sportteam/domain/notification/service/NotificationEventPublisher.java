package com.back.sportteam.domain.notification.service;

import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.notification.kafka.topic.match:notification.match}")
    private String matchNotificationTopic;

    public void publishMatchConfirmed(String matchId, LocalDateTime occurredAt) {
        publish(matchId, NotificationType.MATCH_CONFIRMED, occurredAt);
    }

    public void publishMatchCancelled(String matchId, LocalDateTime occurredAt) {
        publish(matchId, NotificationType.MATCH_CANCELLED, occurredAt);
    }

    public void publishMatchReminder(String matchId, LocalDateTime occurredAt) {
        publish(matchId, NotificationType.MATCH_REMINDER, occurredAt);
    }

    private void publish(String matchId, NotificationType type, LocalDateTime occurredAt) {
        MatchNotificationEvent event = new MatchNotificationEvent(matchId, type, occurredAt);
        try {
            kafkaTemplate.send(matchNotificationTopic, matchId, objectMapper.writeValueAsString(event))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error(
                                    "Failed to publish notification event. matchId={}, type={}",
                                    matchId,
                                    type,
                                    throwable
                            );
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification event.", e);
        }
    }
}
