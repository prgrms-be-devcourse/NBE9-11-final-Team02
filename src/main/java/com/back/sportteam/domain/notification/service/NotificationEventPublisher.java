package com.back.sportteam.domain.notification.service;

import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

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
        applicationEventPublisher.publishEvent(new MatchNotificationEvent(matchId, type, occurredAt));
    }
}
