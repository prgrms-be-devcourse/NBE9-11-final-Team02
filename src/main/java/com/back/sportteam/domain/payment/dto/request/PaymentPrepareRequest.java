package com.back.sportteam.domain.payment.dto.request;

import com.back.sportteam.domain.payment.entity.PaymentType;

public record PaymentPrepareRequest(
        Long matchId,
        Long amount,
        PaymentType paymentType
) {
}
