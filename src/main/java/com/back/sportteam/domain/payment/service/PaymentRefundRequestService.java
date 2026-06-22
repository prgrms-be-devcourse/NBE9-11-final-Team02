package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentRefundRequestService {

    public static final String MATCH_CANCELLED_BY_HOST = "MATCH_CANCELLED_BY_HOST";
    public static final String MATCH_MINIMUM_PARTICIPANTS_NOT_MET = "MATCH_MINIMUM_PARTICIPANTS_NOT_MET";
    public static final String MATCH_PARTICIPANT_LEFT = "MATCH_PARTICIPANT_LEFT";

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ReservationRepository reservationRepository;

    public void requestMatchRefunds(
            String matchId,
            String reservationId,
            String reason,
            LocalDateTime requestedAt
    ) {
        List<Payment> paidPayments = new ArrayList<>();
        paidPayments.addAll(paymentRepository.findAllByMatchIdAndStatus(matchId, PaymentStatus.PAID));
        findFacilitySlotId(reservationId)
                .map(facilitySlotId -> paymentRepository.findAllByFacilitySlotIdAndStatus(
                        facilitySlotId,
                        PaymentStatus.PAID
                ))
                .ifPresent(paidPayments::addAll);
        saveRefunds(paidPayments, reason, requestedAt);
    }

    public void requestParticipantRefunds(String participantId, String reason, LocalDateTime requestedAt) {
        List<Payment> paidPayments = paymentRepository.findAllByParticipantIdAndStatus(participantId, PaymentStatus.PAID);
        saveRefunds(paidPayments, reason, requestedAt);
    }

    private Optional<String> findFacilitySlotId(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return Optional.empty();
        }
        return reservationRepository.findById(reservationId)
                .map(reservation -> reservation.getFacilitySlotId());
    }

    private void saveRefunds(List<Payment> paidPayments, String reason, LocalDateTime requestedAt) {
        List<Refund> refunds = paidPayments.stream()
                .filter(payment -> payment.getPaymentType() == PaymentType.PARTICIPATION
                        || payment.getPaymentType() == PaymentType.FACILITY)
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
