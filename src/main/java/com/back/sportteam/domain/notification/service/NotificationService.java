package com.back.sportteam.domain.notification.service;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.notification.dto.response.NotificationResponse;
import com.back.sportteam.domain.notification.entity.Notification;
import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import com.back.sportteam.domain.notification.exception.NotificationErrorCode;
import com.back.sportteam.domain.notification.repository.NotificationRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.util.TimeUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;

    @Transactional
    public void handle(MatchNotificationEvent event) {
        Match match = matchRepository.findById(event.matchId()).orElse(null);
        if (match == null) {
            return;
        }

        List<Notification> notifications = matchParticipantRepository
                .findByMatchIdAndStatusIn(event.matchId(), notificationTargetStatuses(event.type()))
                .stream()
                .map(MatchParticipant::getUserId)
                .filter(userId -> !notificationRepository.existsByUserIdAndTypeAndReferenceId(
                        userId,
                        event.type(),
                        event.matchId()
                ))
                .map(userId -> createNotification(userId, match, event))
                .toList();

        if (!notifications.isEmpty()) {
            List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
            sendNotificationsAfterCommit(savedNotifications);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isOwnedBy(userId)) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markRead(LocalDateTime.now(TimeUtils.SERVICE_ZONE));
        return NotificationResponse.from(notification);
    }

    private List<MatchParticipantStatus> notificationTargetStatuses(NotificationType type) {
        if (type == NotificationType.MATCH_CANCELLED) {
            return List.of(MatchParticipantStatus.ACTIVE, MatchParticipantStatus.CANCELLED);
        }
        return List.of(MatchParticipantStatus.ACTIVE);
    }

    private Notification createNotification(String userId, Match match, MatchNotificationEvent event) {
        return Notification.match(
                userId,
                event.type(),
                title(event.type()),
                message(match, event.type()),
                event.matchId(),
                event.occurredAt()
        );
    }

    private void sendNotificationsAfterCommit(List<Notification> notifications) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifications.forEach(notificationSseService::sendToUser);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifications.forEach(notificationSseService::sendToUser);
            }
        });
    }

    private String title(NotificationType type) {
        return switch (type) {
            case MATCH_CONFIRMED -> "경기가 확정되었습니다.";
            case MATCH_CANCELLED -> "경기가 취소되었습니다.";
            case MATCH_REMINDER -> "경기 시작 1시간 전입니다.";
        };
    }

    private String message(Match match, NotificationType type) {
        return switch (type) {
            case MATCH_CONFIRMED -> "'%s' 경기가 모집 완료되어 확정되었습니다.".formatted(match.getTitle());
            case MATCH_CANCELLED -> "'%s' 경기가 취소되었습니다. 필요한 결제는 환불 절차가 진행됩니다.".formatted(match.getTitle());
            case MATCH_REMINDER -> "'%s' 경기가 1시간 후 시작됩니다.".formatted(match.getTitle());
        };
    }
}
