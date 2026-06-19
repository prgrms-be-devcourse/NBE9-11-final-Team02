package com.back.sportteam.domain.payment.dto.response;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;

public record PaymentConfirmResponse(
        String merchantUid,
        String paymentKey,
        Integer amount,
        PaymentStatus status
) {

    public static PaymentConfirmResponse from(Payment payment) {
        return new PaymentConfirmResponse(
                payment.getMerchantUid(),
                payment.getPgTransactionId(),
                payment.getAmount(),
                payment.getStatus()
        );
    }
}
