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
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "refunds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_refunds_one_pending_per_payment",
                columnNames = {"payment_id", "pending_flag"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    @Column(
            name = "pending_flag",
            insertable = false,
            updatable = false,
            columnDefinition = "TINYINT GENERATED ALWAYS AS "
                    + "(CASE WHEN status IN ('PENDING', 'PROCESSING') THEN 1 ELSE NULL END)"
    )
    private Integer pendingFlag;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    private Refund(Payment payment, Integer amount, String reason, LocalDateTime requestedAt) {
        this.id = UUID.randomUUID().toString();
        this.payment = payment;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
        this.requestedAt = requestedAt;
        this.retryCount = 0;
    }

    public static Refund pending(
            Payment payment,
            Integer amount,
            String reason,
            LocalDateTime requestedAt
    ) {
        return new Refund(payment, amount, reason, requestedAt);
    }

    public boolean isPending() {
        return status == RefundStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == RefundStatus.PROCESSING;
    }

    public void markProcessing(LocalDateTime attemptedAt) {
        if (status != RefundStatus.PENDING) {
            throw new IllegalStateException("Only PENDING refunds can be marked PROCESSING.");
        }

        this.status = RefundStatus.PROCESSING;
        this.nextRetryAt = null;
        this.lastAttemptedAt = attemptedAt;
        this.failureReason = null;
    }

    public void complete(LocalDateTime completedAt) {
        if (status == RefundStatus.COMPLETED) {
            return;
        }
        if (status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING refunds can be completed.");
        }

        this.status = RefundStatus.COMPLETED;
        this.completedAt = completedAt;
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        if (status == RefundStatus.FAILED) {
            return;
        }
        if (status != RefundStatus.PENDING && status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Only PENDING or PROCESSING refunds can be failed.");
        }

        this.status = RefundStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void recordFailure(
            String failureReason,
            LocalDateTime attemptedAt,
            int maxRetryCount,
            Duration retryDelay
    ) {
        if (status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING refunds can record failure.");
        }

        int nextRetryCount = retryCount + 1;
        this.retryCount = nextRetryCount;
        this.failureReason = failureReason;
        this.lastAttemptedAt = attemptedAt;

        if (nextRetryCount >= maxRetryCount) {
            this.status = RefundStatus.FAILED;
            this.nextRetryAt = null;
            return;
        }

        this.status = RefundStatus.PENDING;
        this.nextRetryAt = attemptedAt.plus(retryDelay);
    }

    public void failPermanently(String failureReason, LocalDateTime attemptedAt) {
        if (status == RefundStatus.FAILED) {
            return;
        }
        if (status != RefundStatus.PENDING && status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Only PENDING or PROCESSING refunds can be failed.");
        }

        this.status = RefundStatus.FAILED;
        this.failureReason = failureReason;
        this.lastAttemptedAt = attemptedAt;
        this.nextRetryAt = null;
    }
}
