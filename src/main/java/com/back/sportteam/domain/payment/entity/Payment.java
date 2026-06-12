package com.back.sportteam.domain.payment.entity;

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
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "participant_id", columnDefinition = "CHAR(36)")
    private String participantId;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(name = "match_id", columnDefinition = "CHAR(36)", nullable = false)
    private String matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "merchant_uid", nullable = false, unique = true, length = 100)
    private String merchantUid;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_provider", nullable = false, length = 30)
    private PaymentProvider pgProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    private Payment(
            String participantId,
            String userId,
            String matchId,
            PaymentType paymentType,
            String merchantUid,
            Integer amount
    ) {
        this.id = UUID.randomUUID().toString();
        this.participantId = participantId;
        this.userId = userId;
        this.matchId = matchId;
        this.paymentType = paymentType;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.pgProvider = PaymentProvider.TOSSPAYMENTS;
        this.status = PaymentStatus.PENDING;
    }

    public static Payment create(
            String participantId,
            String userId,
            String matchId,
            PaymentType paymentType,
            String merchantUid,
            Integer amount
    ) {
        return new Payment(participantId, userId, matchId, paymentType, merchantUid, amount);
    }
}
