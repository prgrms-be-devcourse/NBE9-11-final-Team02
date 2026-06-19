package com.back.sportteam.infra.payment.toss;

public record TossPaymentsConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}
