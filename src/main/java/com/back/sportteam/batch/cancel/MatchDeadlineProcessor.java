package com.back.sportteam.batch.cancel;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.domain.notification.service.NotificationEventPublisher;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import com.back.sportteam.domain.reservation.service.ReservationSlotService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchDeadlineProcessor {

    private static final String MINIMUM_PARTICIPANTS_NOT_MET = "MATCH_MINIMUM_PARTICIPANTS_NOT_MET";

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSlotService reservationSlotService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public void process(String matchId, LocalDateTime processedAt) {
        Match match = matchRepository.findByIdForUpdate(matchId).orElse(null);
        if (match == null || !match.isRecruiting() || !match.isRecruitClosed(processedAt)) {
            return;
        }

        if (match.hasEnoughParticipants()) {
            match.confirm(processedAt);
            reservationSlotService.confirmReservation(match.getReservationId());
            notificationEventPublisher.publishMatchConfirmed(match.getId(), processedAt);
            return;
        }

        match.cancel(processedAt);
        reservationSlotService.cancelReservation(match.getReservationId(), processedAt);
        cancelActiveParticipants(matchId);
        enqueueRefunds(match, processedAt);
        notificationEventPublisher.publishMatchCancelled(match.getId(), processedAt);
    }

    private void cancelActiveParticipants(String matchId) {
        matchParticipantRepository.findByMatchIdAndStatus(
                        matchId,
                        MatchParticipantStatus.ACTIVE
                )
                .forEach(MatchParticipant::cancel);
    }

    private void enqueueRefunds(Match match, LocalDateTime requestedAt) {
        List<Payment> paidPayments = new java.util.ArrayList<>(
                paymentRepository.findAllByMatchIdAndStatus(match.getId(), PaymentStatus.PAID)
        );
        reservationRepository.findById(match.getReservationId())
                .map(reservation -> paymentRepository.findAllByFacilitySlotIdAndStatus(
                        reservation.getFacilitySlotId(),
                        PaymentStatus.PAID
                ))
                .ifPresent(paidPayments::addAll);

        List<Refund> refunds = paidPayments.stream()
                .filter(payment -> payment.getAmount() > payment.getRefundedAmount())
                .filter(payment -> !refundRepository.existsByPaymentIdAndStatusIn(
                        payment.getId(),
                        List.of(RefundStatus.PENDING, RefundStatus.PROCESSING)
                ))
                .map(payment -> Refund.pending(
                        payment,
                        payment.getAmount() - payment.getRefundedAmount(),
                        MINIMUM_PARTICIPANTS_NOT_MET,
                        requestedAt
                ))
                .toList();

        if (!refunds.isEmpty()) {
            refundRepository.saveAll(refunds);
        }
    }
}
