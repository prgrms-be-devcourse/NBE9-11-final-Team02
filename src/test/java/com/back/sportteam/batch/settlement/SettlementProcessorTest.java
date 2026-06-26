package com.back.sportteam.batch.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
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
import com.back.sportteam.domain.settlement.entity.SettlementStatus;
import com.back.sportteam.domain.settlement.policy.SettlementPolicy;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
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
    void 완료된_경기의_참가비를_합산하여_정산_테이블에_행을_생성한다() {
        Match match = createCompletedMatch();
        List<String> ids = List.of(match.getId());
        when(matchRepository.findAllById(ids)).thenReturn(List.of(match));
        when(paymentRepository.sumAmountMapByMatchIds(ids, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Map.of(match.getId(), 30_000L));
        when(settlementPolicy.getPlatformFeeRate()).thenReturn(new BigDecimal("0.0700"));

        settlementProcessor.processBatch(ids);

        Settlement saved = captureSaved().get(0);
        assertThat(saved.getMatchId()).isEqualTo(match.getId());
        assertThat(saved.getTotalParticipantFee()).isEqualTo(30_000);
        assertThat(saved.getPlatformFee()).isEqualTo(2_100);
        assertThat(saved.getHostSettlementAmount()).isEqualTo(27_900);
    }

    @Test
    void 완료_상태가_아닌_경기는_정산_테이블에_행을_생성하지_않는다() {
        Match match = createMatch();
        List<String> ids = List.of(match.getId());
        when(matchRepository.findAllById(ids)).thenReturn(List.of(match));
        when(paymentRepository.sumAmountMapByMatchIds(ids, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Map.of());

        settlementProcessor.processBatch(ids);

        assertThat(captureSaved()).isEmpty();
    }

    @Test
    void 조회된_경기가_없으면_예외_없이_아무_행도_생성하지_않는다() {
        List<String> ids = List.of("unknown");
        when(matchRepository.findAllById(ids)).thenReturn(List.of());
        when(paymentRepository.sumAmountMapByMatchIds(ids, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Map.of());

        assertThatNoException().isThrownBy(() -> settlementProcessor.processBatch(ids));

        assertThat(captureSaved()).isEmpty();
    }

    @Test
    void 유료_경기에_결제_내역이_없으면_정산_테이블에_행을_생성하지_않고_에러_로그를_남긴다() {
        Match match = createCompletedMatch();
        List<String> ids = List.of(match.getId());
        when(matchRepository.findAllById(ids)).thenReturn(List.of(match));
        when(paymentRepository.sumAmountMapByMatchIds(ids, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Map.of());

        settlementProcessor.processBatch(ids);

        assertThat(captureSaved()).isEmpty();
    }

    @Test
    void 무료_경기는_0원으로_정산_테이블에_행을_생성한다() {
        Match match = createFreeMatch();
        List<String> ids = List.of(match.getId());
        when(matchRepository.findAllById(ids)).thenReturn(List.of(match));
        when(paymentRepository.sumAmountMapByMatchIds(ids, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Map.of());
        when(settlementPolicy.getPlatformFeeRate()).thenReturn(new BigDecimal("0.0700"));

        settlementProcessor.processBatch(ids);

        List<Settlement> saved = captureSaved();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTotalParticipantFee()).isEqualTo(0);
        assertThat(saved.get(0).getStatus()).isEqualTo(SettlementStatus.SETTLED);
    }

    @Test
    void 수수료는_원_단위_미만을_절사하여_방장에게_유리하게_계산한다() {
        Match match = createCompletedMatch();
        List<String> ids = List.of(match.getId());
        when(matchRepository.findAllById(ids)).thenReturn(List.of(match));
        when(paymentRepository.sumAmountMapByMatchIds(ids, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Map.of(match.getId(), 10_010L));
        when(settlementPolicy.getPlatformFeeRate()).thenReturn(new BigDecimal("0.0700"));

        settlementProcessor.processBatch(ids);

        Settlement saved = captureSaved().get(0);
        // 10010 * 0.07 = 700.7 → 절사(DOWN) → 701이 아니라 700, 방장 몫은 그만큼 늘어난다
        assertThat(saved.getPlatformFee()).isEqualTo(700);
        assertThat(saved.getHostSettlementAmount()).isEqualTo(9_310);
    }

    @Test
    void 한_배치에_정상_경기와_건너뛸_경기가_섞여도_정상_경기만_정산한다() {
        Match completed = createCompletedMatch();
        Match notCompleted = createMatch();
        List<String> ids = List.of(completed.getId(), notCompleted.getId());
        when(matchRepository.findAllById(ids)).thenReturn(List.of(completed, notCompleted));
        when(paymentRepository.sumAmountMapByMatchIds(ids, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Map.of(completed.getId(), 30_000L));
        when(settlementPolicy.getPlatformFeeRate()).thenReturn(new BigDecimal("0.0700"));

        settlementProcessor.processBatch(ids);

        List<Settlement> saved = captureSaved();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getMatchId()).isEqualTo(completed.getId());
    }

    @SuppressWarnings("unchecked")
    private List<Settlement> captureSaved() {
        ArgumentCaptor<List<Settlement>> captor = ArgumentCaptor.forClass(List.class);
        verify(settlementRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Match createCompletedMatch() {
        Match match = createMatch();
        match.confirm(LocalDateTime.now());
        match.complete();
        return match;
    }

    private Match createFreeMatch() {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("무료 풋살")
                .sportType(SportType.FUTSAL)
                .capacity(10)
                .feePerPerson(0)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .matchDate(MATCH_DATE)
                .startTime(MATCH_START_TIME)
                .endTime(MATCH_END_TIME)
                .recruitDeadline(LocalDateTime.of(MATCH_DATE, MATCH_START_TIME).minusHours(1))
                .cancelDeadline(LocalDateTime.of(MATCH_DATE, MATCH_START_TIME).minusHours(1))
                .build());
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
                .participantCancelDeadline(LocalDateTime.of(MATCH_DATE, MATCH_START_TIME).minusHours(1))

                .hostCancelDeadline(LocalDateTime.of(MATCH_DATE, MATCH_START_TIME).minusHours(1))
                .build());
    }
}
