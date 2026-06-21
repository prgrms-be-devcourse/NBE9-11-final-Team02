package com.back.sportteam.domain.settlement.entity;

import com.back.sportteam.domain.match.entity.SportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlements_match_id", columnNames = "match_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "match_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String matchId;

    @Column(name = "host_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String hostId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 20, updatable = false)
    private SportType sportType;

    @Column(name = "total_participant_fee", nullable = false, updatable = false)
    private int totalParticipantFee;

    @Column(name = "applied_fee_rate", nullable = false, precision = 5, scale = 4, updatable = false)
    private BigDecimal appliedFeeRate;

    @Column(name = "platform_fee", nullable = false, updatable = false)
    private int platformFee;

    @Column(name = "host_settlement_amount", nullable = false, updatable = false)
    private int hostSettlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Settlement(
            String matchId,
            String hostId,
            SportType sportType,
            int totalParticipantFee,
            BigDecimal appliedFeeRate
    ) {
        validate(matchId, hostId, sportType, totalParticipantFee, appliedFeeRate);

        this.id = UUID.randomUUID().toString();
        this.matchId = matchId;
        this.hostId = hostId;
        this.sportType = sportType;
        this.totalParticipantFee = totalParticipantFee;
        this.appliedFeeRate = appliedFeeRate;
        this.platformFee = calculatePlatformFee(totalParticipantFee, appliedFeeRate);
        this.hostSettlementAmount = totalParticipantFee - this.platformFee;
        this.status = SettlementStatus.HOLDING;
    }

    public static Settlement create(
            String matchId,
            String hostId,
            SportType sportType,
            int totalParticipantFee,
            BigDecimal appliedFeeRate
    ) {
        return new Settlement(matchId, hostId, sportType, totalParticipantFee, appliedFeeRate);
    }

    public void markSettled(LocalDateTime settledAt) {
        if (status == SettlementStatus.SETTLED) {
            return;
        }
        this.status = SettlementStatus.SETTLED;
        this.settledAt = settledAt;
    }

    public void markFailed() {
        this.status = SettlementStatus.FAILED;
    }

    /*
     * 수수료율 곱셈은 부동소수점 오차를 피하기 위해 BigDecimal로 계산하고,
     * 원 단위 미만은 절사(0 방향, DOWN)하여 방장에게 유리하도록 한 뒤 정수(원)로 변환한다.
     */
    private static int calculatePlatformFee(int totalParticipantFee, BigDecimal appliedFeeRate) {
        return BigDecimal.valueOf(totalParticipantFee)
                .multiply(appliedFeeRate)
                .setScale(0, RoundingMode.DOWN)
                .intValueExact();
    }

    private static void validate(
            String matchId,
            String hostId,
            SportType sportType,
            int totalParticipantFee,
            BigDecimal appliedFeeRate
    ) {
        if (matchId == null || hostId == null || sportType == null) {
            throw new IllegalArgumentException("정산 생성에 필요한 식별자가 누락되었습니다.");
        }
        if (totalParticipantFee < 0) {
            throw new IllegalArgumentException("총 참가비는 0 이상이어야 합니다.");
        }
        if (appliedFeeRate == null
                || appliedFeeRate.signum() < 0
                || appliedFeeRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("수수료율은 0 이상 1 이하여야 합니다.");
        }
    }
}
