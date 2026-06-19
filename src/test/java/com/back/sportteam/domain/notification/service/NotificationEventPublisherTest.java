package com.back.sportteam.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationEventPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final NotificationEventPublisher publisher = new NotificationEventPublisher(kafkaTemplate, objectMapper);

    @Test
    void matchConfirmedEventIsPublishedToKafka() throws Exception {
        ReflectionTestUtils.setField(publisher, "matchNotificationTopic", "notification.match");
        LocalDateTime occurredAt = LocalDateTime.of(2026, Month.JUNE, 18, 10, 0);
        when(kafkaTemplate.send(eq("notification.match"), eq("match-id"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishMatchConfirmed("match-id", occurredAt);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("notification.match"), eq("match-id"), payloadCaptor.capture());

        MatchNotificationEvent event = objectMapper.readValue(payloadCaptor.getValue(), MatchNotificationEvent.class);
        assertThat(event.matchId()).isEqualTo("match-id");
        assertThat(event.type()).isEqualTo(NotificationType.MATCH_CONFIRMED);
        assertThat(event.occurredAt()).isEqualTo(occurredAt);
    }
}
