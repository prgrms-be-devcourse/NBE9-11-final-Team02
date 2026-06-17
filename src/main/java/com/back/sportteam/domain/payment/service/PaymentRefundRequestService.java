package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentRefundRequestService {

    public static final String MATCH_CANCELLED_BY_HOST = "MATCH_CANCELLED_BY_HOST";
    public static final String MATCH_MINIMUM_PARTICIPANTS_NOT_MET = "MATCH_MINIMUM_PARTICIPANTS_NOT_MET";

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public void requestMatchRefunds(String matchId, String reason, LocalDateTime requestedAt) {
        List<Payment> paidPayments = paymentRepository.findAllByMatchIdAndStatus(matchId, PaymentStatus.PAID);

        List<Refund> refunds = paidPayments.stream()
                .filter(payment -> payment.getPaymentType() == PaymentType.PARTICIPATION)
                .filter(payment -> payment.getAmount() > payment.getRefundedAmount())
                .filter(payment -> !refundRepository.existsByPaymentIdAndStatus(
                        payment.getId(),
                        RefundStatus.PENDING
                ))
                .map(payment -> Refund.pending(
                        payment,
                        payment.getAmount() - payment.getRefundedAmount(),
                        reason,
                        requestedAt
                ))
                .toList();

        if (!refunds.isEmpty()) {
            refundRepository.saveAll(refunds);
        }
    }
}
