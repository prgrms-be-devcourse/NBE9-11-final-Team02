package com.back.sportteam.batch.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.settlement.entity.Settlement;
import com.back.sportteam.domain.settlement.policy.SettlementPolicy;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementProcessorTest {

    private static final LocalDate MATCH_DATE = LocalDate.of(2099, Month.JUNE, 10);
    private static final LocalTime MATCH_START_TIME = LocalTime.of(10, 0);
    private static final LocalTime MATCH_END_TIME = LocalTime.of(12, 0);

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementPolicy settlementPolicy;

    @InjectMocks
    private SettlementProcessor settlementProcessor;

    @Test
    void 완료된_경기의_참가비를_합산하여_정산을_생성한다() {
        Match match = createCompletedMatch();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(paymentRepository.sumAmountByMatchId(match.getId(), PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(30_000L);
        when(settlementPolicy.getPlatformFeeRate()).thenReturn(new BigDecimal("0.0700"));

        settlementProcessor.process(match.getId());

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        Settlement saved = captor.getValue();
        assertThat(saved.getMatchId()).isEqualTo(match.getId());
        assertThat(saved.getTotalParticipantFee()).isEqualTo(30_000);
        assertThat(saved.getPlatformFee()).isEqualTo(2_100);
        assertThat(saved.getHostSettlementAmount()).isEqualTo(27_900);
    }

    @Test
    void 완료_상태가_아닌_경기는_정산_생성하지_않는다() {
        Match match = createMatch();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        settlementProcessor.process(match.getId());

        verify(settlementRepository, never()).save(any());
    }

    @Test
    void 경기가_존재하지_않으면_예외없이_무시한다() {
        when(matchRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatNoException()
                .isThrownBy(() -> settlementProcessor.process("unknown"));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    void 지불된_참가비가_없으면_도메인_불변식_위반으로_예외가_발생한다() {
        Match match = createCompletedMatch();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(paymentRepository.sumAmountByMatchId(match.getId(), PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(null);

        String matchId = match.getId();
        assertThatThrownBy(() -> settlementProcessor.process(matchId))
                .isInstanceOf(IllegalStateException.class);

        verify(settlementRepository, never()).save(any());
    }

    private Match createCompletedMatch() {
        Match match = createMatch();
        match.confirm(LocalDateTime.now());
        match.complete();
        return match;
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
                .matchDate(MATCH_DATE)
                .startTime(MATCH_START_TIME)
                .endTime(MATCH_END_TIME)
                .recruitDeadline(LocalDateTime.of(MATCH_DATE, MATCH_START_TIME).minusHours(1))
                .cancelDeadline(LocalDateTime.of(MATCH_DATE, MATCH_START_TIME).minusHours(1))
                .build());
    }
}
