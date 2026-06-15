package com.back.sportteam.domain.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
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
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
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
}
