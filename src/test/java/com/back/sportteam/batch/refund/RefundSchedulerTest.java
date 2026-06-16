package com.back.sportteam.batch.refund;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.domain.payment.service.PaymentRefundProcessor;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefundSchedulerTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRefundProcessor paymentRefundProcessor;

    private RefundScheduler refundScheduler;

    @BeforeEach
    void setUp() {
        refundScheduler = new RefundScheduler(refundRepository, paymentRefundProcessor);
        ReflectionTestUtils.setField(refundScheduler, "batchSize", 50);
        ReflectionTestUtils.setField(refundScheduler, "processingTimeoutMinutes", 10L);
    }

    @Test
    void processPendingRefundsContinuesAfterFailure() {
        when(refundRepository.findIdsByStatus(
                eq(RefundStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of("refund-1", "refund-2"));
        doThrow(new IllegalStateException("processing failed"))
                .when(paymentRefundProcessor)
                .process(eq("refund-1"), any(LocalDateTime.class));

        refundScheduler.processPendingRefunds();

        verify(refundRepository).recoverStaleProcessingRefunds(
                eq(RefundStatus.PROCESSING),
                eq(RefundStatus.PENDING),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(paymentRefundProcessor).process(eq("refund-2"), any(LocalDateTime.class));
    }
}
