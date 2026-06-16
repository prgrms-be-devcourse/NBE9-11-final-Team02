package com.back.sportteam.infra.payment.toss;

public record TossPaymentsCancelRequest(
        String cancelReason,
        Integer cancelAmount
) {
}
