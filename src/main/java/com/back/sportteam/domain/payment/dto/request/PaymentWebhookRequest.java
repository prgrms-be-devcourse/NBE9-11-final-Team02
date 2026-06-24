package com.back.sportteam.domain.payment.dto.request;

import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import java.time.LocalDateTime;

public record PaymentWebhookRequest(
        String eventId,
        PaymentWebhookEventType eventType,
        String merchantUid,
        String pgTransactionId,
        Integer amount,
        LocalDateTime approvedAt
) {
}
