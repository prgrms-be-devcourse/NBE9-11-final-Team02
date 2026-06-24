package com.back.sportteam.domain.payment.dto.response;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentConfirmResponse(
        String merchantUid,
        String paymentKey,
        Integer amount,
        PaymentStatus status,
        LocalDateTime approvedAt
) {

    public static PaymentConfirmResponse from(Payment payment) {
        return new PaymentConfirmResponse(
                payment.getMerchantUid(),
                payment.getPgTransactionId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
