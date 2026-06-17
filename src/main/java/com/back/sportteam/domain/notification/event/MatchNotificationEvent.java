package com.back.sportteam.domain.notification.event;

import com.back.sportteam.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record MatchNotificationEvent(
        String matchId,
        NotificationType type,
        LocalDateTime occurredAt
) {
}
