package com.back.sportteam.infra.payment.toss;

public record TossPaymentsPaymentResponse(
        String paymentKey,
        String orderId,
        String status,
        Integer totalAmount
) {
}
