package com.back.sportteam.infra.payment.toss;

import java.time.OffsetDateTime;

public record TossPaymentsPaymentResponse(
        String paymentKey,
        String orderId,
        String status,
        Integer totalAmount,
        OffsetDateTime approvedAt
) {
}
