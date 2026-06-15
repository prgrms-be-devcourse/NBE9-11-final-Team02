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
                    + "(CASE WHEN status = 'PENDING' THEN 1 ELSE NULL END)"
    )
    private Integer pendingFlag;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    private Refund(Payment payment, Integer amount, String reason, LocalDateTime requestedAt) {
        this.id = UUID.randomUUID().toString();
        this.payment = payment;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public static Refund pending(
            Payment payment,
            Integer amount,
            String reason,
            LocalDateTime requestedAt
    ) {
        return new Refund(payment, amount, reason, requestedAt);
    }
}
