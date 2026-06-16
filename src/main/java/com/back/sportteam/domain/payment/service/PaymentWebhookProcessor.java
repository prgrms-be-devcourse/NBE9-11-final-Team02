package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEvent;
import com.back.sportteam.domain.payment.entity.PaymentWebhookProcessingResult;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.PaymentWebhookEventRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentWebhookProcessor {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Transactional
    public void process(PaymentWebhookRequest request) {
        if (paymentWebhookEventRepository.existsByEventId(request.eventId())) {
            return;
        }

        Payment payment = paymentRepository.findByMerchantUidForUpdate(request.merchantUid())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        validateAmount(payment, request.amount());

        LocalDateTime processedAt = LocalDateTime.now(SERVICE_ZONE);
        PaymentStatus previousStatus = payment.getStatus();
        WebhookProcessingResult result = changePaymentStatus(payment, request, processedAt);
        paymentWebhookEventRepository.saveAndFlush(PaymentWebhookEvent.create(
                payment,
                request.eventId(),
                request.eventType(),
                previousStatus,
                payment.getStatus(),
                result.processingResult(),
                normalize(request.pgTransactionId()),
                request.amount(),
                result.reason(),
                processedAt
        ));
    }

    private void validateAmount(Payment payment, Integer webhookAmount) {
        if (!payment.getAmount().equals(webhookAmount)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private WebhookProcessingResult changePaymentStatus(
            Payment payment,
            PaymentWebhookRequest request,
            LocalDateTime processedAt
    ) {
        if (shouldIgnoreFailureWebhook(payment, request)) {
            return WebhookProcessingResult.ignored("Already terminal payment status.");
        }

        try {
            if (request.eventType().getPaymentStatus() == PaymentStatus.PAID) {
                payment.complete(request.pgTransactionId(), processedAt);
                return WebhookProcessingResult.applied();
            }
            payment.fail(normalize(request.pgTransactionId()));
            return WebhookProcessingResult.applied();
        } catch (IllegalStateException _) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    private boolean shouldIgnoreFailureWebhook(Payment payment, PaymentWebhookRequest request) {
        PaymentStatus requestedStatus = request.eventType().getPaymentStatus();
        return requestedStatus == PaymentStatus.FAILED
                && (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.REFUNDED);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record WebhookProcessingResult(
            PaymentWebhookProcessingResult processingResult,
            String reason
    ) {

        private static WebhookProcessingResult applied() {
            return new WebhookProcessingResult(PaymentWebhookProcessingResult.APPLIED, null);
        }

        private static WebhookProcessingResult ignored(String reason) {
            return new WebhookProcessingResult(PaymentWebhookProcessingResult.IGNORED, reason);
        }
    }
}
