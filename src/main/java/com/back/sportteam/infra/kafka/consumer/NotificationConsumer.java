package com.back.sportteam.infra.kafka.consumer;

import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import com.back.sportteam.domain.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.notification.kafka.topic.match:notification.match}",
            groupId = "${spring.kafka.consumer.group-id:sportteam-notification}"
    )
    public void consume(String payload) {
        MatchNotificationEvent event = parse(payload);
        try {
            notificationService.handle(event);
        } catch (DataIntegrityViolationException e) {
            log.info(
                    "Duplicated notification event ignored. matchId={}, type={}",
                    event.matchId(),
                    event.type()
            );
        }
    }

    private MatchNotificationEvent parse(String payload) {
        try {
            return objectMapper.readValue(payload, MatchNotificationEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize notification event.", e);
        }
    }
}
