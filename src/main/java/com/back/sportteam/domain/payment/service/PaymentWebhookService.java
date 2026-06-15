package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentWebhookResponse;
import com.back.sportteam.domain.payment.repository.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final PaymentWebhookProcessor paymentWebhookProcessor;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;

    public PaymentWebhookResponse handle(PaymentWebhookRequest request) {
        try {
            paymentWebhookProcessor.process(request);
        } catch (DataIntegrityViolationException e) {
            if (!paymentWebhookEventRepository.existsByEventId(request.eventId())) {
                throw e;
            }
        }

        return PaymentWebhookResponse.ok();
    }
}
