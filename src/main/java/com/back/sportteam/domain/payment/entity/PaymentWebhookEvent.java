package com.back.sportteam.domain.payment.entity;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_payment_status", nullable = false, length = 20)
    private PaymentStatus previousPaymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_payment_status", nullable = false, length = 20)
    private PaymentStatus finalPaymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_result", nullable = false, length = 20)
    private PaymentWebhookProcessingResult processingResult;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "result_reason", length = 255)
    private String resultReason;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PaymentWebhookEvent(
            Payment payment,
            String eventId,
            PaymentWebhookEventType eventType,
            PaymentStatus previousPaymentStatus,
            PaymentStatus finalPaymentStatus,
            PaymentWebhookProcessingResult processingResult,
            String pgTransactionId,
            Integer amount,
            String resultReason,
            LocalDateTime processedAt
    ) {
        this.id = UUID.randomUUID().toString();
        this.payment = payment;
        this.eventId = eventId;
        this.eventType = eventType;
        this.paymentStatus = eventType.getPaymentStatus();
        this.previousPaymentStatus = previousPaymentStatus;
        this.finalPaymentStatus = finalPaymentStatus;
        this.processingResult = processingResult;
        this.pgTransactionId = pgTransactionId;
        this.amount = amount;
        this.resultReason = resultReason;
        this.processedAt = processedAt;
        this.createdAt = processedAt;
    }

    public static PaymentWebhookEvent create(
            Payment payment,
            String eventId,
            PaymentWebhookEventType eventType,
            PaymentStatus previousPaymentStatus,
            PaymentStatus finalPaymentStatus,
            PaymentWebhookProcessingResult processingResult,
            String pgTransactionId,
            Integer amount,
            String resultReason,
            LocalDateTime processedAt
    ) {
        return new PaymentWebhookEvent(
                payment,
                eventId,
                eventType,
                previousPaymentStatus,
                finalPaymentStatus,
                processingResult,
                pgTransactionId,
                amount,
                resultReason,
                processedAt
        );
    }
}
