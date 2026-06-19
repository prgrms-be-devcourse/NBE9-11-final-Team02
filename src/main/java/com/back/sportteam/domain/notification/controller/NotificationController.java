package com.back.sportteam.domain.notification.controller;

import com.back.sportteam.domain.notification.dto.response.NotificationResponse;
import com.back.sportteam.domain.notification.service.NotificationService;
import com.back.sportteam.domain.notification.service.NotificationSseService;
import com.back.sportteam.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getNotifications(userId)));
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal String userId
    ) {
        return notificationSseService.subscribe(userId);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @AuthenticationPrincipal String userId,
            @PathVariable String notificationId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.markRead(userId, notificationId)));
    }
}
