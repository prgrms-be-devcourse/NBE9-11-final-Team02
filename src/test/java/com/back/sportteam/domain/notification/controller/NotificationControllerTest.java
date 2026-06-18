package com.back.sportteam.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.notification.dto.response.NotificationResponse;
import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.service.NotificationService;
import com.back.sportteam.domain.notification.service.NotificationSseService;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationControllerTest {

    private final NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
    private final NotificationSseService notificationSseService = org.mockito.Mockito.mock(NotificationSseService.class);
    private final NotificationController notificationController =
            new NotificationController(notificationService, notificationSseService);

    @Test
    void getNotificationsReturnsUserNotifications() {
        NotificationResponse response = response("notification-id");
        when(notificationService.getNotifications("user-id")).thenReturn(List.of(response));

        var result = notificationController.getNotifications("user-id");

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).containsExactly(response);
    }

    @Test
    void subscribeReturnsSseEmitter() {
        SseEmitter emitter = new SseEmitter();
        when(notificationSseService.subscribe("user-id")).thenReturn(emitter);

        SseEmitter result = notificationController.subscribe("user-id");

        assertThat(result).isSameAs(emitter);
    }

    @Test
    void markReadReturnsUpdatedNotification() {
        NotificationResponse response = response("notification-id");
        when(notificationService.markRead("user-id", "notification-id")).thenReturn(response);

        var result = notificationController.markRead("user-id", "notification-id");

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(response);
        verify(notificationService).markRead("user-id", "notification-id");
    }

    private NotificationResponse response(String notificationId) {
        return new NotificationResponse(
                notificationId,
                NotificationType.MATCH_CONFIRMED,
                "title",
                "message",
                "MATCH",
                "match-id",
                false,
                LocalDateTime.of(2026, Month.JUNE, 18, 10, 0),
                null
        );
    }
}
