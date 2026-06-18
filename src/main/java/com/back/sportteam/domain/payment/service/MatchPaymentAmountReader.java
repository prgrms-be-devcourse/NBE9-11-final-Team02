package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.reservation.exception.ReservationErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MatchPaymentAmountReader implements PaymentAmountReader {

    private final MatchRepository matchRepository;
    private final FacilitySlotRepository facilitySlotRepository;

    @Override
    @Transactional(readOnly = true)
    public Integer getFacilityAmount(String facilitySlotId) {
        FacilitySlot facilitySlot = facilitySlotRepository.findById(facilitySlotId)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND));
        if (!isPayableFacilitySlot(facilitySlot)) {
            throw new BusinessException(ReservationErrorCode.SLOT_NOT_AVAILABLE);
        }
        return facilitySlot.getPrice();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getParticipationAmount(String matchId) {
        return getMatch(matchId).getFeePerPerson();
    }

    private Match getMatch(String matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));
    }

    private boolean isPayableFacilitySlot(FacilitySlot facilitySlot) {
        return facilitySlot.getStatus() == SlotStatus.AVAILABLE
                || facilitySlot.getStatus() == SlotStatus.PENDING;
    }
}
