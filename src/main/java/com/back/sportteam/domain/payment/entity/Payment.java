package com.back.sportteam.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "merchant_uid", nullable = false, unique = true, length = 64)
    private String merchantUid;

    @Column(name = "match_id", columnDefinition = "CHAR(36)", nullable = false)
    private String matchId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Payment(
            String merchantUid,
            String matchId,
            Long amount,
            PaymentType paymentType
    ) {
        this.id = UUID.randomUUID().toString();
        this.merchantUid = merchantUid;
        this.matchId = matchId;
        this.amount = amount;
        this.paymentType = paymentType;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now(SERVICE_ZONE);
    }

    public static Payment create(
            String merchantUid,
            String matchId,
            Long amount,
            PaymentType paymentType
    ) {
        return new Payment(merchantUid, matchId, amount, paymentType);
    }
}
