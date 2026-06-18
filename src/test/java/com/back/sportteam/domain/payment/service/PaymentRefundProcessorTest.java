package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentRefundProcessorTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PaymentRefundProcessor paymentRefundProcessor;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void processCompletesPendingRefund() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 16, 12, 0);
        Payment payment = createPaidPayment(10_000);
        Refund refund = Refund.pending(payment, 10_000, "MATCH_CANCELLED", processedAt.minusMinutes(1));
        when(refundRepository.findByIdForUpdate(refund.getId())).thenReturn(Optional.of(refund));

        paymentRefundProcessor.process(refund.getId(), processedAt);

        verify(tossPaymentsClient).cancelPayment("payment-key", 10_000, "MATCH_CANCELLED");
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(refund.getCompletedAt()).isEqualTo(processedAt);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAmount()).isEqualTo(10_000);
        assertThat(payment.getRefundedAt()).isEqualTo(processedAt);
    }

    @Test
    void processFailsRefundWhenPaymentKeyIsMissing() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 16, 12, 0);
        Payment payment = createPendingPayment();
        Refund refund = Refund.pending(payment, 10_000, "MATCH_CANCELLED", processedAt.minusMinutes(1));
        when(refundRepository.findByIdForUpdate(refund.getId())).thenReturn(Optional.of(refund));

        paymentRefundProcessor.process(refund.getId(), processedAt);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
        assertThat(refund.getFailureReason()).isNotBlank();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void processFailsRefundWhenTossPaymentsCancelFails() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 16, 12, 0);
        Payment payment = createPaidPayment(10_000);
        Refund refund = Refund.pending(payment, 10_000, "MATCH_CANCELLED", processedAt.minusMinutes(1));
        when(refundRepository.findByIdForUpdate(refund.getId())).thenReturn(Optional.of(refund));
        doThrow(new BusinessException(PaymentErrorCode.REFUND_FAILED))
                .when(tossPaymentsClient)
                .cancelPayment("payment-key", 10_000, "MATCH_CANCELLED");

        paymentRefundProcessor.process(refund.getId(), processedAt);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getFailureReason()).isNotBlank();
        assertThat(refund.getRetryCount()).isEqualTo(1);
        assertThat(refund.getNextRetryAt()).isEqualTo(processedAt.plusMinutes(1));
        assertThat(refund.getLastAttemptedAt()).isEqualTo(processedAt);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getRefundedAmount()).isZero();
    }

    private Payment createPaidPayment(int amount) {
        Payment payment = createPendingPayment(amount);
        payment.complete("payment-key", LocalDateTime.of(2026, Month.JUNE, 16, 11, 0));
        return payment;
    }

    private Payment createPendingPayment() {
        return createPendingPayment(10_000);
    }

    private Payment createPendingPayment(int amount) {
        return Payment.create(
                "participant-id",
                "user-id",
                "match-id",
                null,
                PaymentType.PARTICIPATION,
                "mid_12345",
                amount
        );
    }
}
