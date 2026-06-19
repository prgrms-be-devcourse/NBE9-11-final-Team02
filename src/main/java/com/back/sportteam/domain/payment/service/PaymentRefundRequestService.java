package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentRefundRequestService {

    public static final String MATCH_CANCELLED_BY_HOST = "MATCH_CANCELLED_BY_HOST";
    public static final String MATCH_MINIMUM_PARTICIPANTS_NOT_MET = "MATCH_MINIMUM_PARTICIPANTS_NOT_MET";
    public static final String MATCH_PARTICIPANT_LEFT = "MATCH_PARTICIPANT_LEFT";
    private static final List<RefundStatus> IN_PROGRESS_REFUND_STATUSES = List.of(
            RefundStatus.PENDING,
            RefundStatus.PROCESSING
    );

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public void requestMatchRefunds(
            String matchId,
            String facilitySlotId,
            String reason,
            LocalDateTime requestedAt
    ) {
        List<Payment> paidPayments = new ArrayList<>();
        paidPayments.addAll(paymentRepository.findAllByMatchIdAndStatus(matchId, PaymentStatus.PAID));
        paidPayments.addAll(paymentRepository.findAllByFacilitySlotIdAndStatus(facilitySlotId, PaymentStatus.PAID));
        saveRefunds(paidPayments, reason, requestedAt);
    }

    public void requestParticipantRefunds(String participantId, String reason, LocalDateTime requestedAt) {
        List<Payment> paidPayments = paymentRepository.findAllByParticipantIdAndStatus(participantId, PaymentStatus.PAID);
        saveRefunds(paidPayments, reason, requestedAt);
    }

    private void saveRefunds(List<Payment> paidPayments, String reason, LocalDateTime requestedAt) {
        List<Refund> refunds = paidPayments.stream()
                .filter(payment -> payment.getPaymentType() == PaymentType.PARTICIPATION
                        || payment.getPaymentType() == PaymentType.FACILITY)
                .filter(payment -> payment.getAmount() > payment.getRefundedAmount())
                .filter(payment -> !refundRepository.existsByPaymentIdAndStatusIn(
                        payment.getId(),
                        IN_PROGRESS_REFUND_STATUSES
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
