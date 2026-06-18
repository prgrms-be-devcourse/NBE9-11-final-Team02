package com.back.sportteam.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationSseServiceTest {

    private final NotificationSseService notificationSseService = new NotificationSseService();

    @Test
    void subscribeReturnsEmitter() {
        SseEmitter emitter = notificationSseService.subscribe("user-id");

        assertThat(emitter).isNotNull();
    }
}
