package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import com.back.sportteam.domain.payment.repository.PaymentWebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class PaymentWebhookServiceTest {

    private final PaymentWebhookProcessor paymentWebhookProcessor =
            org.mockito.Mockito.mock(PaymentWebhookProcessor.class);
    private final PaymentWebhookEventRepository paymentWebhookEventRepository =
            org.mockito.Mockito.mock(PaymentWebhookEventRepository.class);
    private final PaymentWebhookService paymentWebhookService =
            new PaymentWebhookService(paymentWebhookProcessor, paymentWebhookEventRepository);

    @Test
    void duplicateWebhookEventReturnsOkWhenEventAlreadyExists() {
        PaymentWebhookRequest request = request();
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate event"))
                .when(paymentWebhookProcessor).process(request);
        when(paymentWebhookEventRepository.existsByEventId("transmission-id")).thenReturn(true);

        assertThat(paymentWebhookService.handle(request).status()).isEqualTo("OK");
    }

    @Test
    void dataIntegrityExceptionIsRethrownWhenEventDoesNotExist() {
        PaymentWebhookRequest request = request();
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("unexpected constraint"))
                .when(paymentWebhookProcessor).process(request);
        when(paymentWebhookEventRepository.existsByEventId("transmission-id")).thenReturn(false);

        assertThatThrownBy(() -> paymentWebhookService.handle(request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PaymentWebhookRequest request() {
        return new PaymentWebhookRequest(
                "transmission-id",
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                "mid_12345",
                "payment-key",
                10_000
        );
    }
}
