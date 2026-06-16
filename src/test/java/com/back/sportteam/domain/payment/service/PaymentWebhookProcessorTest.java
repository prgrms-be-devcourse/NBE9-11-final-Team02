package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEvent;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import com.back.sportteam.domain.payment.entity.PaymentWebhookProcessingResult;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.PaymentWebhookEventRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @InjectMocks
    private PaymentWebhookProcessor paymentWebhookProcessor;

    @Test
    void succeededWebhookChangesPaymentToPaidAndStoresEvent() {
        Payment payment = createPendingPayment();
        PaymentWebhookRequest request = createRequest(
                "event-1",
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                "pg-transaction-1",
                10_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));

        paymentWebhookProcessor.process(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPgTransactionId()).isEqualTo("pg-transaction-1");
        assertThat(payment.getPaidAt()).isNotNull();

        ArgumentCaptor<PaymentWebhookEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository).saveAndFlush(eventCaptor.capture());
        PaymentWebhookEvent event = eventCaptor.getValue();
        assertThat(event.getEventId()).isEqualTo("event-1");
        assertThat(event.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(event.getPreviousPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(event.getFinalPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(event.getProcessingResult()).isEqualTo(PaymentWebhookProcessingResult.APPLIED);
        assertThat(event.getPgTransactionId()).isEqualTo("pg-transaction-1");
        assertThat(event.getAmount()).isEqualTo(10_000);
    }

    @Test
    void failedWebhookChangesPaymentToFailedAndStoresEvent() {
        Payment payment = createPendingPayment();
        PaymentWebhookRequest request = createRequest(
                "event-2",
                PaymentWebhookEventType.PAYMENT_FAILED,
                null,
                10_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));

        paymentWebhookProcessor.process(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPgTransactionId()).isNull();
        ArgumentCaptor<PaymentWebhookEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository).saveAndFlush(eventCaptor.capture());
        PaymentWebhookEvent event = eventCaptor.getValue();
        assertThat(event.getPreviousPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(event.getFinalPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(event.getProcessingResult()).isEqualTo(PaymentWebhookProcessingResult.APPLIED);
    }

    @Test
    void alreadyProcessedEventIsIgnored() {
        PaymentWebhookRequest request = createRequest(
                "event-1",
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                "pg-transaction-1",
                10_000
        );
        when(paymentWebhookEventRepository.existsByEventId("event-1")).thenReturn(true);

        paymentWebhookProcessor.process(request);

        verify(paymentRepository, never()).findByMerchantUidForUpdate(any());
        verify(paymentWebhookEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void amountMismatchDoesNotChangePaymentStatus() {
        Payment payment = createPendingPayment();
        PaymentWebhookRequest request = createRequest(
                "event-1",
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                "pg-transaction-1",
                9_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentWebhookProcessor.process(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentWebhookEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void paidPaymentIgnoresFailedWebhookAndStoresEvent() {
        Payment payment = createPendingPayment();
        payment.complete("pg-transaction-1", LocalDateTime.of(2026, Month.JUNE, 16, 12, 0));
        PaymentWebhookRequest request = createRequest(
                "event-3",
                PaymentWebhookEventType.PAYMENT_FAILED,
                null,
                10_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));

        paymentWebhookProcessor.process(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPgTransactionId()).isEqualTo("pg-transaction-1");
        ArgumentCaptor<PaymentWebhookEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository).saveAndFlush(eventCaptor.capture());
        PaymentWebhookEvent event = eventCaptor.getValue();
        assertThat(event.getPreviousPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(event.getFinalPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(event.getProcessingResult()).isEqualTo(PaymentWebhookProcessingResult.IGNORED);
        assertThat(event.getResultReason()).isNotBlank();
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

    private PaymentWebhookRequest createRequest(
            String eventId,
            PaymentWebhookEventType eventType,
            String pgTransactionId,
            Integer amount
    ) {
        return new PaymentWebhookRequest(
                eventId,
                eventType,
                "mid_12345",
                pgTransactionId,
                amount
        );
    }
}
