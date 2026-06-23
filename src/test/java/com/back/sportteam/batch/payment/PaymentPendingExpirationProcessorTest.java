package com.back.sportteam.batch.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.service.PaymentPostProcessor;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PaymentPendingExpirationProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPostProcessor paymentPostProcessor;

    @Test
    void expireStalePendingPaymentsChangesStatusAndRunsPostProcessor() {
        PaymentPendingExpirationProcessor processor = new PaymentPendingExpirationProcessor(
                paymentRepository,
                paymentPostProcessor
        );
        Payment payment = Payment.create(
                "participant-id",
                "user-id",
                "match-id",
                null,
                PaymentType.PARTICIPATION,
                "mid_12345",
                10_000
        );
        LocalDateTime threshold = LocalDateTime.of(2026, Month.JUNE, 23, 10, 0);
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 23, 10, 30);
        when(paymentRepository.findStalePendingPaymentsForUpdate(
                PaymentStatus.PENDING,
                threshold,
                PageRequest.of(0, 100)
        )).thenReturn(List.of(payment));

        int processedCount = processor.expire(threshold, processedAt, 100);

        assertThat(processedCount).isEqualTo(1);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPgTransactionId()).isNull();
        verify(paymentPostProcessor).processFailedPayment(payment, processedAt);
    }
}
