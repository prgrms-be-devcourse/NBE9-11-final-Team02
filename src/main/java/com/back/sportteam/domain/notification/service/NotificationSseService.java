package com.back.sportteam.domain.notification.service;

import com.back.sportteam.domain.notification.dto.response.NotificationResponse;
import com.back.sportteam.domain.notification.entity.Notification;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationSseService {

    private static final long DEFAULT_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final String CONNECT_EVENT_NAME = "connect";
    private static final String NOTIFICATION_EVENT_NAME = "notification";

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ignored -> removeEmitter(userId, emitter));

        sendConnectEvent(userId, emitter);
        return emitter;
    }

    public void sendToUser(Notification notification) {
        List<SseEmitter> userEmitters = emitters.get(notification.getUserId());
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        NotificationResponse response = NotificationResponse.from(notification);
        userEmitters.forEach(emitter -> sendNotification(notification.getUserId(), emitter, response));
    }

    private void sendConnectEvent(String userId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name(CONNECT_EVENT_NAME)
                    .data("connected"));
        } catch (IOException _) {
            removeEmitter(userId, emitter);
        }
    }

    private void sendNotification(String userId, SseEmitter emitter, NotificationResponse response) {
        try {
            emitter.send(SseEmitter.event()
                    .name(NOTIFICATION_EVENT_NAME)
                    .id(response.notificationId())
                    .data(response));
        } catch (IOException _) {
            removeEmitter(userId, emitter);
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }
}
