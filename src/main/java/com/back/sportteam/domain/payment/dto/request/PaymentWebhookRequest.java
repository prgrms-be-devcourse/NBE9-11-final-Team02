package com.back.sportteam.domain.payment.dto.request;

import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;

public record PaymentWebhookRequest(
        String eventId,
        PaymentWebhookEventType eventType,
        String merchantUid,
        String pgTransactionId,
        Integer amount
) {
}
