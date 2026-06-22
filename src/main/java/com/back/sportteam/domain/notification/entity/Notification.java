package com.back.sportteam.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "reference_id", columnDefinition = "CHAR(36)")
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Notification(
            String userId,
            NotificationType type,
            String title,
            String content,
            String referenceId,
            LocalDateTime createdAt
    ) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.referenceId = referenceId;
        this.status = NotificationStatus.PENDING;
        this.read = false;
        this.retryCount = 0;
        this.createdAt = createdAt;
    }

    public static Notification match(
            String userId,
            NotificationType type,
            String title,
            String content,
            String matchId,
            LocalDateTime createdAt
    ) {
        return new Notification(userId, type, title, content, matchId, createdAt);
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.read = true;
            this.readAt = readAt;
        }
    }

    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }
}
