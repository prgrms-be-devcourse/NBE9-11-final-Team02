package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEvent;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.PaymentWebhookEventRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.util.TimeUtils;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentWebhookProcessor {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final PaymentPostProcessor paymentPostProcessor;

    @Transactional
    public void process(PaymentWebhookRequest request) {
        if (paymentWebhookEventRepository.existsByEventId(request.eventId())) {
            return;
        }

        Payment payment = paymentRepository.findByMerchantUidForUpdate(request.merchantUid())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        validateAmount(payment, request.amount());

        LocalDateTime processedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
        changePaymentStatus(payment, request, processedAt);
        paymentWebhookEventRepository.saveAndFlush(PaymentWebhookEvent.create(
                payment,
                request,
                processedAt
        ));
    }

    private void validateAmount(Payment payment, Integer webhookAmount) {
        if (!payment.getAmount().equals(webhookAmount)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void changePaymentStatus(
            Payment payment,
            PaymentWebhookRequest request,
            LocalDateTime processedAt
    ) {
        if (shouldIgnoreFailureWebhook(payment, request)) {
            return;
        }

        try {
            if (request.eventType().getPaymentStatus() == PaymentStatus.PAID) {
                LocalDateTime approvedAt = getApprovedAt(request, processedAt);
                payment.complete(request.pgTransactionId(), approvedAt);
                paymentPostProcessor.processPaidPayment(payment, approvedAt);
                return;
            }
            payment.fail(normalize(request.pgTransactionId()));
            paymentPostProcessor.processFailedPayment(payment, processedAt);
        } catch (IllegalStateException _) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    private LocalDateTime getApprovedAt(PaymentWebhookRequest request, LocalDateTime processedAt) {
        if (request.approvedAt() == null) {
            return processedAt;
        }
        return request.approvedAt();
    }

    private boolean shouldIgnoreFailureWebhook(Payment payment, PaymentWebhookRequest request) {
        return request.eventType().getPaymentStatus() == PaymentStatus.FAILED
                && (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.REFUNDED);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
