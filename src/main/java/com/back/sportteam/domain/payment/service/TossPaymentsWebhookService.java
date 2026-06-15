package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentWebhookResponse;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import com.back.sportteam.infra.payment.toss.TossPaymentsPaymentResponse;
import com.back.sportteam.infra.payment.toss.TossPaymentsWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TossPaymentsWebhookService {

    private static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";

    private final ObjectMapper objectMapper;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookResponse handle(String transmissionId, String payload) {
        if (!hasText(transmissionId)) {
            throw new BusinessException(PaymentErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }

        TossPaymentsWebhookRequest webhook = parse(payload);
        PaymentWebhookEventType eventType = mapEventType(webhook);
        if (eventType == null) {
            return PaymentWebhookResponse.ok();
        }

        TossPaymentsWebhookRequest.TossPaymentsData data = webhook.data();
        TossPaymentsPaymentResponse payment = tossPaymentsClient.getPayment(data.paymentKey());
        verifyPayment(data, payment);

        return paymentWebhookService.handle(new PaymentWebhookRequest(
                transmissionId,
                eventType,
                data.orderId(),
                data.paymentKey(),
                data.totalAmount()
        ));
    }

    private TossPaymentsWebhookRequest parse(String payload) {
        try {
            TossPaymentsWebhookRequest request =
                    objectMapper.readValue(payload, TossPaymentsWebhookRequest.class);
            if (request == null
                    || !PAYMENT_STATUS_CHANGED.equals(request.eventType())
                    || request.data() == null
                    || !hasText(request.data().paymentKey())
                    || !hasText(request.data().orderId())
                    || !hasText(request.data().status())
                    || request.data().totalAmount() == null
                    || request.data().totalAmount() <= 0) {
                throw new BusinessException(PaymentErrorCode.INVALID_WEBHOOK_PAYLOAD);
            }
            return request;
        } catch (JacksonException e) {
            throw new BusinessException(PaymentErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
    }

    private PaymentWebhookEventType mapEventType(TossPaymentsWebhookRequest webhook) {
        String status = webhook.data().status();
        if ("DONE".equals(status)) {
            return PaymentWebhookEventType.PAYMENT_SUCCEEDED;
        }
        if ("ABORTED".equals(status) || "EXPIRED".equals(status)) {
            return PaymentWebhookEventType.PAYMENT_FAILED;
        }
        return null;
    }

    private void verifyPayment(
            TossPaymentsWebhookRequest.TossPaymentsData webhook,
            TossPaymentsPaymentResponse payment
    ) {
        if (payment == null
                || !webhook.paymentKey().equals(payment.paymentKey())
                || !webhook.orderId().equals(payment.orderId())
                || !webhook.status().equals(payment.status())
                || !webhook.totalAmount().equals(payment.totalAmount())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_PROVIDER_VERIFICATION_FAILED);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
