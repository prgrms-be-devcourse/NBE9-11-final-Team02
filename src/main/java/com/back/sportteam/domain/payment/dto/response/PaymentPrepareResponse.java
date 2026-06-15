package com.back.sportteam.domain.payment.dto.response;

import com.back.sportteam.domain.payment.entity.Payment;

public record PaymentPrepareResponse(
        String merchantUid,
        Integer amount
) {

    public static PaymentPrepareResponse from(Payment payment) {
        return new PaymentPrepareResponse(
                payment.getMerchantUid(),
                payment.getAmount()
        );
    }
}
