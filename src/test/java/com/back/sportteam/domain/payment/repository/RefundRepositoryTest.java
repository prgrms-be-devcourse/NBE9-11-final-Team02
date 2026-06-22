package com.back.sportteam.domain.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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
        Payment payment = savePayment("match-id", "mid_12345");
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
    void 완료된_환불은_새_PENDING_환불을_막지_않는다() {
        Payment payment = savePayment("match-id-2", "mid_23456");
        LocalDateTime requestedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Refund completedRefund = Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        );
        completedRefund.complete(requestedAt.plusMinutes(1));
        refundRepository.saveAndFlush(completedRefund);

        Refund nextRefund = Refund.pending(
                payment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt.plusMinutes(2)
        );
        refundRepository.saveAndFlush(nextRefund);

        assertThat(refundRepository.findAll()).hasSize(2);
    }

    @Test
    void 처리_대상_PENDING_환불_ID를_요청순으로_조회한다() {
        Payment firstPayment = savePayment("match-id-3", "mid_34567");
        Payment secondPayment = savePayment("match-id-4", "mid_45678");
        LocalDateTime requestedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Refund firstRefund = refundRepository.save(Refund.pending(
                firstPayment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt
        ));
        Refund secondRefund = refundRepository.save(Refund.pending(
                secondPayment,
                10_000,
                "MATCH_MINIMUM_PARTICIPANTS_NOT_MET",
                requestedAt.plusMinutes(1)
        ));

        assertThat(refundRepository.findProcessableIds(RefundStatus.PENDING, PageRequest.of(0, 10)))
                .containsExactly(firstRefund.getId(), secondRefund.getId());
    }

    private Payment savePayment(String matchId, String merchantUid) {
        return paymentRepository.save(Payment.create(
                "participant-id-" + matchId,
                "user-id",
                matchId,
                null,
                PaymentType.PARTICIPATION,
                merchantUid,
                10_000
        ));
    }
}
