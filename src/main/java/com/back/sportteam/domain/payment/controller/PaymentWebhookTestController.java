package com.back.sportteam.domain.payment.controller;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentWebhookResponse;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.service.PaymentWebhookProcessor;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Profile("dev")
@RequiredArgsConstructor
@RequestMapping("/api/v1/dev/payments")
public class PaymentWebhookTestController {

    private static final String DEV_EVENT_PREFIX = "dev_event_";
    private static final String DEV_TRANSACTION_PREFIX = "dev_pg_";

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookProcessor paymentWebhookProcessor;

    @PostMapping("/{merchantUid}/success")
    public ResponseEntity<ApiResponse<PaymentWebhookResponse>> success(@PathVariable String merchantUid) {
        Payment payment = getPayment(merchantUid);
        paymentWebhookProcessor.process(new PaymentWebhookRequest(
                generateEventId(),
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                merchantUid,
                generateTransactionId(),
                payment.getAmount()
        ));
        return ResponseEntity.ok(ApiResponse.ok(PaymentWebhookResponse.ok()));
    }

    @PostMapping("/{merchantUid}/fail")
    public ResponseEntity<ApiResponse<PaymentWebhookResponse>> fail(@PathVariable String merchantUid) {
        Payment payment = getPayment(merchantUid);
        paymentWebhookProcessor.process(new PaymentWebhookRequest(
                generateEventId(),
                PaymentWebhookEventType.PAYMENT_FAILED,
                merchantUid,
                null,
                payment.getAmount()
        ));
        return ResponseEntity.ok(ApiResponse.ok(PaymentWebhookResponse.ok()));
    }

    private Payment getPayment(String merchantUid) {
        return paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private String generateEventId() {
        return DEV_EVENT_PREFIX + UUID.randomUUID();
    }

    private String generateTransactionId() {
        return DEV_TRANSACTION_PREFIX + UUID.randomUUID();
    }
}
