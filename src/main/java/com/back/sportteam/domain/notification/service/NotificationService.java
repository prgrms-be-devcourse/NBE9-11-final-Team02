package com.back.sportteam.domain.notification.service;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.notification.entity.Notification;
import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import com.back.sportteam.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(MatchNotificationEvent event) {
        Match match = matchRepository.findById(event.matchId()).orElse(null);
        if (match == null) {
            return;
        }

        List<Notification> notifications = matchParticipantRepository
                .findByMatchIdAndStatus(event.matchId(), MatchParticipantStatus.ACTIVE)
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
            notificationRepository.saveAll(notifications);
        }
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
            case MATCH_CANCELLED -> "'%s' 경기가 취소되었습니다. 환불이 필요한 결제는 순차 처리됩니다.".formatted(match.getTitle());
            case MATCH_REMINDER -> "'%s' 경기가 1시간 후 시작됩니다.".formatted(match.getTitle());
        };
    }
}
