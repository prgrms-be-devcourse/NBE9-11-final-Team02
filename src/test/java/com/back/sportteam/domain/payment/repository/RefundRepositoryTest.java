package com.back.sportteam.domain.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefundRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Test
    void 결제별_PENDING_환불은_한_건만_저장할_수_있다() {
        Payment payment = paymentRepository.save(Payment.create(
                "participant-id",
                "user-id",
                "match-id",
                null,
                PaymentType.PARTICIPATION,
                "mid_12345",
                10_000
        ));
        LocalDateTime requestedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        refundRepository.saveAndFlush(Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        ));

        assertThat(refundRepository.existsByPaymentIdAndStatus(
                payment.getId(),
                RefundStatus.PENDING
        )).isTrue();

        Refund duplicate = Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        );
        assertThatThrownBy(() -> refundRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void processingRefundAlsoBlocksDuplicatePendingRefund() {
        Payment payment = paymentRepository.save(Payment.create(
                "participant-id",
                "user-id",
                "match-id-2",
                null,
                PaymentType.PARTICIPATION,
                "mid_23456",
                10_000
        ));
        LocalDateTime requestedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Refund processingRefund = Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        );
        processingRefund.markProcessing(requestedAt.plusMinutes(1));
        refundRepository.saveAndFlush(processingRefund);

        assertThat(refundRepository.existsByPaymentIdAndStatusIn(
                payment.getId(),
                List.of(RefundStatus.PENDING, RefundStatus.PROCESSING)
        )).isTrue();

        Refund duplicate = Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        );
        assertThatThrownBy(() -> refundRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findProcessableIdsExcludesRefundsScheduledForFutureRetry() {
        Payment payment = paymentRepository.save(Payment.create(
                "participant-id",
                "user-id",
                "match-id-3",
                null,
                PaymentType.PARTICIPATION,
                "mid_34567",
                10_000
        ));
        LocalDateTime requestedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Refund refund = Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        );
        refund.markProcessing(requestedAt.plusMinutes(1));
        refund.recordFailure(
                "temporary failure",
                requestedAt.plusMinutes(1),
                3,
                Duration.ofMinutes(5)
        );
        refundRepository.saveAndFlush(refund);

        List<String> beforeRetryTime = refundRepository.findProcessableIds(
                RefundStatus.PENDING,
                requestedAt.plusMinutes(3),
                PageRequest.of(0, 10)
        );
        List<String> afterRetryTime = refundRepository.findProcessableIds(
                RefundStatus.PENDING,
                requestedAt.plusMinutes(6),
                PageRequest.of(0, 10)
        );

        assertThat(beforeRetryTime).isEmpty();
        assertThat(afterRetryTime)
                .isNotEmpty()
                .contains(refund.getId());
    }

    @Test
    void recoverStaleProcessingRefundsMovesStaleProcessingRefundToPending() {
        Payment payment = paymentRepository.save(Payment.create(
                "participant-id",
                "user-id",
                "match-id-4",
                null,
                PaymentType.PARTICIPATION,
                "mid_45678",
                10_000
        ));
        LocalDateTime requestedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        LocalDateTime now = requestedAt.plusMinutes(20);
        Refund refund = Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        );
        refund.markProcessing(requestedAt.plusMinutes(1));
        refundRepository.saveAndFlush(refund);

        int recoveredCount = refundRepository.recoverStaleProcessingRefunds(
                RefundStatus.PROCESSING,
                RefundStatus.PENDING,
                now.minusMinutes(10),
                now
        );
        Refund recoveredRefund = refundRepository.findById(refund.getId()).orElseThrow();

        assertThat(recoveredCount).isEqualTo(1);
        assertThat(recoveredRefund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(recoveredRefund.getNextRetryAt()).isEqualTo(now);
    }
}
