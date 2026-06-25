package com.back.sportteam.infra.payment.toss;

import java.time.OffsetDateTime;

public record TossPaymentsWebhookRequest(
        String eventType,
        TossPaymentsData data
) {

    public record TossPaymentsData(
            String paymentKey,
            String orderId,
            String status,
            Integer totalAmount,
            OffsetDateTime approvedAt
    ) {
    }
}
