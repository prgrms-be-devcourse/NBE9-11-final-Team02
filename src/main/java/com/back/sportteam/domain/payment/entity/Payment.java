package com.back.sportteam.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_uid", nullable = false, unique = true, length = 64)
    private String merchantUid;

    @Column(name = "match_id")
    private Long matchId;

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

    protected Payment() {
    }

    private Payment(
            String merchantUid,
            Long matchId,
            Long amount,
            PaymentType paymentType
    ) {
        this.merchantUid = merchantUid;
        this.matchId = matchId;
        this.amount = amount;
        this.paymentType = paymentType;
        this.status = PaymentStatus.READY;
        this.createdAt = LocalDateTime.now();
    }

    public static Payment create(
            String merchantUid,
            Long matchId,
            Long amount,
            PaymentType paymentType
    ) {
        return new Payment(merchantUid, matchId, amount, paymentType);
    }

    public Long getId() {
        return id;
    }

    public String getMerchantUid() {
        return merchantUid;
    }

    public Long getMatchId() {
        return matchId;
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
