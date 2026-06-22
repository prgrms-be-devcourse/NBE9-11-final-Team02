package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentPostProcessor {

    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchRepository matchRepository;
    private final FacilitySlotRepository facilitySlotRepository;
    private final ReservationRepository reservationRepository;

    public void processPaidPayment(Payment payment) {
        if (payment.getPaymentType() == PaymentType.PARTICIPATION) {
            activateParticipant(payment);
            return;
        }

        confirmFacilitySlot(payment);
    }

    public void processFailedPayment(Payment payment, LocalDateTime processedAt) {
        if (payment.getPaymentType() == PaymentType.PARTICIPATION) {
            cancelParticipant(payment, processedAt);
            return;
        }

        cancelMatchForFacilityPayment(payment, processedAt);
    }

    private void activateParticipant(Payment payment) {
        MatchParticipant participant = matchParticipantRepository.findById(payment.getParticipantId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_PARTICIPANT_NOT_FOUND));
        participant.activate();
    }

    private void confirmFacilitySlot(Payment payment) {
        FacilitySlot facilitySlot = getFacilitySlot(payment.getFacilitySlotId());
        facilitySlot.reserve();
    }

    private void cancelParticipant(Payment payment, LocalDateTime processedAt) {
        MatchParticipant participant = matchParticipantRepository.findById(payment.getParticipantId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_PARTICIPANT_NOT_FOUND));
        if (participant.cancel()) {
            participant.getMatch().decreaseCurrentCount();
        }
        if (participant.isHost()) {
            participant.getMatch().cancel(processedAt);
        }
    }

    private void cancelMatchForFacilityPayment(Payment payment, LocalDateTime processedAt) {
        FacilitySlot facilitySlot = getFacilitySlot(payment.getFacilitySlotId());
        facilitySlot.release();

        Match match = getMatchByFacilitySlotId(payment.getFacilitySlotId());
        matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                match.getId(),
                payment.getUserId(),
                MatchParticipantStatus.ACTIVE
        ).ifPresent(participant -> {
            if (participant.cancel()) {
                match.decreaseCurrentCount();
            }
        });
        match.cancel(processedAt);
    }

    private FacilitySlot getFacilitySlot(String facilitySlotId) {
        return facilitySlotRepository.findByIdForUpdate(facilitySlotId)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND));
    }

    private Match getMatchByFacilitySlotId(String facilitySlotId) {
        Reservation reservation = reservationRepository.findByFacilitySlotId(facilitySlotId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        return matchRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));
    }
}
