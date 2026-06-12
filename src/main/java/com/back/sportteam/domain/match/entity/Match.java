package com.back.sportteam.domain.match.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "matches",
        uniqueConstraints = @UniqueConstraint(name = "uk_matches_reservation_id", columnNames = "reservation_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "reservation_id", columnDefinition = "CHAR(36)", nullable = false)
    private String reservationId;

    @Column(name = "host_id", columnDefinition = "CHAR(36)", nullable = false)
    private String hostId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 20)
    private SportType sportType;

    @Column(name = "min_participants", nullable = false)
    private int minParticipants;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "current_count", nullable = false)
    private int currentCount;

    @Column(name = "fee_per_person", nullable = false)
    private int feePerPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_skill_level", nullable = false, length = 10)
    private SkillLevel minSkillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_skill_level", nullable = false, length = 10)
    private SkillLevel maxSkillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_gender", nullable = false, length = 10)
    private RequiredGender requiredGender;

    @Column(name = "cancel_deadline", nullable = false)
    private LocalDateTime cancelDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private MatchStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Match(MatchCreateCommand command) {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        this.id = UUID.randomUUID().toString();
        this.reservationId = command.getReservationId();
        this.hostId = command.getHostId();
        this.title = command.getTitle();
        this.sportType = command.getSportType();
        this.minParticipants = command.getMinParticipants();
        this.maxParticipants = command.getMaxParticipants();
        this.currentCount = 1;
        this.feePerPerson = command.getFeePerPerson();
        this.minSkillLevel = defaultSkillLevel(command.getMinSkillLevel());
        this.maxSkillLevel = defaultSkillLevel(command.getMaxSkillLevel());
        this.requiredGender = defaultRequiredGender(command.getRequiredGender());
        this.cancelDeadline = command.getCancelDeadline();
        this.status = MatchStatus.RECRUITING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Match create(MatchCreateCommand command) {
        validateParticipantRange(command.getMinParticipants(), command.getMaxParticipants());
        return new Match(command);
    }

    public boolean isRecruiting() {
        return status == MatchStatus.RECRUITING;
    }

    public boolean isFull() {
        return currentCount >= maxParticipants;
    }

    public void increaseCurrentCount() {
        this.currentCount++;
    }

    public void decreaseCurrentCount() {
        if (currentCount > 0) {
            this.currentCount--;
        }
    }

    private static void validateParticipantRange(int minParticipants, int maxParticipants) {
        if (minParticipants > maxParticipants) {
            throw new IllegalArgumentException("최소 인원은 최대 인원보다 클 수 없습니다.");
        }
    }

    private static SkillLevel defaultSkillLevel(SkillLevel skillLevel) {
        if (skillLevel == null) {
            return SkillLevel.ANY;
        }
        return skillLevel;
    }

    private static RequiredGender defaultRequiredGender(RequiredGender requiredGender) {
        if (requiredGender == null) {
            return RequiredGender.ANY;
        }
        return requiredGender;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now(SERVICE_ZONE);
    }
}
