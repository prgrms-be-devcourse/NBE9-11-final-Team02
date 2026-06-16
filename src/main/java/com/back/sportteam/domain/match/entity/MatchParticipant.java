package com.back.sportteam.domain.match.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Entity
@Table(name = "match_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchParticipant {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration PAYMENT_HOLD_DURATION = Duration.ofMinutes(10);

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MatchParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchParticipantStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    private MatchParticipant(
            Match match,
            String userId,
            MatchParticipantRole role,
            MatchParticipantStatus status,
            LocalDateTime joinedAt,
            LocalDateTime paymentDeadline
    ) {
        this.id = UUID.randomUUID().toString();
        this.match = match;
        this.userId = userId;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
        this.paymentDeadline = paymentDeadline;
    }

    public static MatchParticipant host(Match match, String userId) {
        return new MatchParticipant(
                match,
                userId,
                MatchParticipantRole.HOST,
                MatchParticipantStatus.ACTIVE,
                LocalDateTime.now(SERVICE_ZONE),
                null
        );
    }

    public static MatchParticipant participant(Match match, String userId) {
        LocalDateTime joinedAt = LocalDateTime.now(SERVICE_ZONE);
        return new MatchParticipant(
                match,
                userId,
                MatchParticipantRole.PARTICIPANT,
                MatchParticipantStatus.PAYMENT_PENDING,
                joinedAt,
                joinedAt.plus(PAYMENT_HOLD_DURATION)
        );
    }

    public boolean isHost() {
        return role == MatchParticipantRole.HOST;
    }

    public void cancel() {
        this.status = MatchParticipantStatus.CANCELLED;
        this.paymentDeadline = null;
    }
}
