package com.back.sportteam.infra.kafka.consumer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import com.back.sportteam.domain.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class NotificationConsumerTest {

    private final NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final NotificationConsumer notificationConsumer = new NotificationConsumer(notificationService, objectMapper);

    @Test
    void kafkaPayloadIsHandledAsMatchNotificationEvent() throws Exception {
        MatchNotificationEvent event = new MatchNotificationEvent(
                "match-id",
                NotificationType.MATCH_REMINDER,
                LocalDateTime.of(2026, Month.JUNE, 18, 10, 0)
        );

        notificationConsumer.consume(objectMapper.writeValueAsString(event));

        verify(notificationService).handle(event);
    }

    @Test
    void duplicatedNotificationEventIsIgnored() throws Exception {
        MatchNotificationEvent event = new MatchNotificationEvent(
                "match-id",
                NotificationType.MATCH_CANCELLED,
                LocalDateTime.of(2026, Month.JUNE, 18, 10, 0)
        );
        String payload = objectMapper.writeValueAsString(event);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicated"))
                .when(notificationService)
                .handle(event);

        notificationConsumer.consume(payload);

        verify(notificationService).handle(event);
        verifyNoMoreInteractions(notificationService);
    }
}
