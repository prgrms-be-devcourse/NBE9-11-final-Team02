package com.back.sportteam.domain.payment.entity;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_webhook_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEvent {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private PaymentWebhookEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PaymentWebhookEvent(
            Payment payment,
            PaymentWebhookRequest request,
            LocalDateTime processedAt
    ) {
        this.id = UUID.randomUUID().toString();
        this.payment = payment;
        this.eventId = request.eventId();
        this.eventType = request.eventType();
        this.paymentStatus = request.eventType().getPaymentStatus();
        this.amount = request.amount();
        this.processedAt = processedAt;
        this.createdAt = processedAt;
    }

    public static PaymentWebhookEvent create(
            Payment payment,
            PaymentWebhookRequest request,
            LocalDateTime processedAt
    ) {
        return new PaymentWebhookEvent(
                payment,
                request,
                processedAt
        );
    }
}
