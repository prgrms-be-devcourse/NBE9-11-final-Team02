package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchPaymentAmountReaderTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private FacilitySlotRepository facilitySlotRepository;

    @InjectMocks
    private MatchPaymentAmountReader paymentAmountReader;

    @Test
    void 참가_결제_금액은_매칭의_인당_참가비로_조회한다() {
        Match match = mock(Match.class);
        when(match.getFeePerPerson()).thenReturn(10_000);
        when(matchRepository.findById("match-id")).thenReturn(Optional.of(match));

        Integer amount = paymentAmountReader.getParticipationAmount("match-id");

        assertThat(amount).isEqualTo(10_000);
    }

    @Test
    void 시설_결제_금액은_시설_슬롯_가격으로_조회한다() {
        FacilitySlot facilitySlot = mock(FacilitySlot.class);
        when(facilitySlot.getPrice()).thenReturn(100_000);
        when(facilitySlotRepository.findById("slot-id")).thenReturn(Optional.of(facilitySlot));

        Integer amount = paymentAmountReader.getFacilityAmount("slot-id");

        assertThat(amount).isEqualTo(100_000);
    }

    @Test
    void 존재하지_않는_매칭의_결제_금액은_조회할_수_없다() {
        when(matchRepository.findById("match-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentAmountReader.getParticipationAmount("match-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }
}
