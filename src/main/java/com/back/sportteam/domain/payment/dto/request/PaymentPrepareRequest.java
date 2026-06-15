package com.back.sportteam.domain.payment.dto.request;

import com.back.sportteam.domain.payment.entity.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentPrepareRequest(
        String matchId,

        String facilitySlotId,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0원보다 커야 합니다.")
        Integer amount,

        @NotNull(message = "결제 유형은 필수입니다.")
        PaymentType paymentType
) {
}
