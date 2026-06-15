package com.back.sportteam.infra.payment.toss;

public record TossPaymentsWebhookRequest(
        String eventType,
        TossPaymentsData data
) {

    public record TossPaymentsData(
            String paymentKey,
            String orderId,
            String status,
            Integer totalAmount
    ) {
    }
}
