package com.back.sportteam.domain.payment.controller;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentWebhookResponse;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.service.PaymentWebhookProcessor;
import com.back.sportteam.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentWebhookTestControllerTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentWebhookProcessor paymentWebhookProcessor = mock(PaymentWebhookProcessor.class);
    private final PaymentWebhookTestController controller =
            new PaymentWebhookTestController(paymentRepository, paymentWebhookProcessor);

    @Test
    void dev_결제_성공_웹훅을_처리한다() {
        Payment payment = createPendingPayment();
        when(paymentRepository.findByMerchantUid("mid_12345")).thenReturn(Optional.of(payment));

        ResponseEntity<ApiResponse<PaymentWebhookResponse>> response = controller.success("mid_12345");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        ArgumentCaptor<PaymentWebhookRequest> requestCaptor = ArgumentCaptor.forClass(PaymentWebhookRequest.class);
        verify(paymentWebhookProcessor).process(requestCaptor.capture());
        PaymentWebhookRequest request = requestCaptor.getValue();
        assertThat(request.eventId()).startsWith("dev_event_");
        assertThat(request.eventType()).isEqualTo(PaymentWebhookEventType.PAYMENT_SUCCEEDED);
        assertThat(request.merchantUid()).isEqualTo("mid_12345");
        assertThat(request.pgTransactionId()).startsWith("dev_pg_");
        assertThat(request.amount()).isEqualTo(10_000);
    }

    @Test
    void dev_결제_실패_웹훅을_처리한다() {
        Payment payment = createPendingPayment();
        when(paymentRepository.findByMerchantUid("mid_12345")).thenReturn(Optional.of(payment));

        ResponseEntity<ApiResponse<PaymentWebhookResponse>> response = controller.fail("mid_12345");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        ArgumentCaptor<PaymentWebhookRequest> requestCaptor = ArgumentCaptor.forClass(PaymentWebhookRequest.class);
        verify(paymentWebhookProcessor).process(requestCaptor.capture());
        PaymentWebhookRequest request = requestCaptor.getValue();
        assertThat(request.eventId()).startsWith("dev_event_");
        assertThat(request.eventType()).isEqualTo(PaymentWebhookEventType.PAYMENT_FAILED);
        assertThat(request.merchantUid()).isEqualTo("mid_12345");
        assertThat(request.pgTransactionId()).isNull();
        assertThat(request.amount()).isEqualTo(10_000);
    }

    private Payment createPendingPayment() {
        return Payment.create(
                "participant-id",
                "user-id",
                "match-id",
                null,
                PaymentType.PARTICIPATION,
                "mid_12345",
                10_000
        );
    }
}
