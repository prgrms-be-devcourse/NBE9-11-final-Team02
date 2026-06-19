package com.back.sportteam.domain.notification.dto.response;

import com.back.sportteam.domain.notification.entity.Notification;
import com.back.sportteam.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        String notificationId,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        String referenceId,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
