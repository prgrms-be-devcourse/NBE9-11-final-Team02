package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.reservation.exception.ReservationErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchPaymentAmountReaderTest {

    private final MatchRepository matchRepository = org.mockito.Mockito.mock(MatchRepository.class);
    private final FacilitySlotRepository facilitySlotRepository = org.mockito.Mockito.mock(FacilitySlotRepository.class);
    private final MatchPaymentAmountReader amountReader =
            new MatchPaymentAmountReader(matchRepository, facilitySlotRepository);

    @Test
    void availableFacilitySlotAmountCanBeRead() {
        FacilitySlot slot = createSlot();
        when(facilitySlotRepository.findById("slot-id")).thenReturn(Optional.of(slot));

        assertThat(amountReader.getFacilityAmount("slot-id")).isEqualTo(100_000);
    }

    @Test
    void pendingFacilitySlotAmountCanBeReadForHostPayment() {
        FacilitySlot slot = createSlot();
        slot.holdUntil(LocalDateTime.of(2026, Month.JUNE, 18, 10, 10));
        when(facilitySlotRepository.findById("slot-id")).thenReturn(Optional.of(slot));

        assertThat(amountReader.getFacilityAmount("slot-id")).isEqualTo(100_000);
    }

    @Test
    void reservedFacilitySlotCannotBePreparedAgain() {
        FacilitySlot slot = createSlot();
        slot.reserve();
        when(facilitySlotRepository.findById("slot-id")).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> amountReader.getFacilityAmount("slot-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.SLOT_NOT_AVAILABLE);
    }

    @Test
    void missingFacilitySlotThrows() {
        when(facilitySlotRepository.findById("slot-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> amountReader.getFacilityAmount("slot-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND);
    }

    @Test
    void participationAmountUsesMatchFeePerPerson() {
        Match match = createMatch();
        when(matchRepository.findById("match-id")).thenReturn(Optional.of(match));

        assertThat(amountReader.getParticipationAmount("match-id")).isEqualTo(10_000);
    }

    @Test
    void missingMatchThrows() {
        when(matchRepository.findById("match-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> amountReader.getParticipationAmount("match-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    private FacilitySlot createSlot() {
        return FacilitySlot.create(
                "facility-id",
                LocalDate.of(2026, Month.JUNE, 20),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                100_000
        );
    }

    private Match createMatch() {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .capacity(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(LocalDateTime.of(2026, Month.JUNE, 19, 10, 0))
                .participantCancelDeadline(LocalDateTime.of(2026, Month.JUNE, 19, 11, 0))

                .hostCancelDeadline(LocalDateTime.of(2026, Month.JUNE, 19, 11, 0))
                .build());
    }
}
