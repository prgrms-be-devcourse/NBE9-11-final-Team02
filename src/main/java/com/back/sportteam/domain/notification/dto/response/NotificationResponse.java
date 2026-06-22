package com.back.sportteam.domain.notification.dto.response;

import com.back.sportteam.domain.notification.entity.Notification;
import com.back.sportteam.domain.notification.entity.NotificationStatus;
import com.back.sportteam.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        String notificationId,
        NotificationType type,
        String title,
        String content,
        String referenceId,
        NotificationStatus status,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReferenceId(),
                notification.getStatus(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
