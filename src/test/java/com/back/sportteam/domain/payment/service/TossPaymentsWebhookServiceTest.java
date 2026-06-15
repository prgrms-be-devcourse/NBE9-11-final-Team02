package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentWebhookResponse;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import com.back.sportteam.infra.payment.toss.TossPaymentsPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TossPaymentsWebhookServiceTest {

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private PaymentWebhookService paymentWebhookService;

    private TossPaymentsWebhookService tossPaymentsWebhookService;

    @BeforeEach
    void setUp() {
        tossPaymentsWebhookService = new TossPaymentsWebhookService(
                new ObjectMapper(),
                tossPaymentsClient,
                paymentWebhookService
        );
    }

    @Test
    void DONE_웹훅이면_토스_결제를_재조회한_뒤_PAID_이벤트로_처리한다() {
        when(tossPaymentsClient.getPayment("payment-key")).thenReturn(
                new TossPaymentsPaymentResponse("payment-key", "mid_12345", "DONE", 10_000)
        );
        when(paymentWebhookService.handle(any())).thenReturn(PaymentWebhookResponse.ok());

        tossPaymentsWebhookService.handle("transmission-1", donePayload());

        ArgumentCaptor<PaymentWebhookRequest> captor =
                ArgumentCaptor.forClass(PaymentWebhookRequest.class);
        verify(paymentWebhookService).handle(captor.capture());
        PaymentWebhookRequest request = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.eventId()).isEqualTo("transmission-1");
        org.assertj.core.api.Assertions.assertThat(request.eventType())
                .isEqualTo(PaymentWebhookEventType.PAYMENT_SUCCEEDED);
        org.assertj.core.api.Assertions.assertThat(request.pgTransactionId()).isEqualTo("payment-key");
    }

    @Test
    void 토스_재조회_결과가_웹훅과_다르면_처리하지_않는다() {
        when(tossPaymentsClient.getPayment("payment-key")).thenReturn(
                new TossPaymentsPaymentResponse("payment-key", "mid_12345", "DONE", 9_000)
        );

        assertThatThrownBy(() -> tossPaymentsWebhookService.handle("transmission-1", donePayload()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_PROVIDER_VERIFICATION_FAILED);

        verify(paymentWebhookService, never()).handle(any());
    }

    @Test
    void 아직_종료되지_않은_토스_상태는_결제_상태를_변경하지_않는다() {
        String payload = donePayload().replace("\"DONE\"", "\"IN_PROGRESS\"");

        tossPaymentsWebhookService.handle("transmission-1", payload);

        verify(tossPaymentsClient, never()).getPayment(any());
        verify(paymentWebhookService, never()).handle(any());
    }

    private String donePayload() {
        return """
                {
                  "eventType": "PAYMENT_STATUS_CHANGED",
                  "data": {
                    "paymentKey": "payment-key",
                    "orderId": "mid_12345",
                    "status": "DONE",
                    "totalAmount": 10000
                  }
                }
                """;
    }
}
