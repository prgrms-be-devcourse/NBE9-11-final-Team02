package com.back.sportteam.domain.settlement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.settlement.entity.Settlement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class SettlementRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2099, Month.JUNE, 10);
    private static final PageRequest PAGE = PageRequest.of(0, 1000);

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Test
    void 정산_테이블에_행이_없는_어제까지_완료된_경기를_조회한다() {
        Match completed = persistMatch("res-1", TODAY.minusDays(1), MatchStatus.COMPLETED);

        List<String> result = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PAGE);

        assertThat(result).containsExactly(completed.getId());
    }

    @Test
    void 이미_정산_테이블에_행이_있는_경기는_정산_배치_대상에_포함되지_않는다() {
        Match settled = persistMatch("res-1", TODAY.minusDays(1), MatchStatus.COMPLETED);
        persistSettlement(settled.getId());

        List<String> result = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PAGE);

        assertThat(result).isEmpty();
    }

    @Test
    void 완료_상태가_아닌_경기는_조회되지_않는다() {
        persistMatch("res-1", TODAY.minusDays(1), MatchStatus.CONFIRMED);

        List<String> result = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PAGE);

        assertThat(result).isEmpty();
    }

    @Test
    void 정산_배치는_전날까지_완료된_경기만_대상으로_한다() {
        persistMatch("res-today", TODAY, MatchStatus.COMPLETED);
        persistMatch("res-future", TODAY.plusDays(1), MatchStatus.COMPLETED);

        List<String> result = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PAGE);

        assertThat(result).isEmpty();
    }

    @Test
    void 경기_날짜가_이른_순서로_정렬한다() {
        Match late = persistMatch("res-late", TODAY.minusDays(1), MatchStatus.COMPLETED);
        Match early = persistMatch("res-early", TODAY.minusDays(3), MatchStatus.COMPLETED);
        Match middle = persistMatch("res-middle", TODAY.minusDays(2), MatchStatus.COMPLETED);

        List<String> result = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PAGE);

        assertThat(result).containsExactly(early.getId(), middle.getId(), late.getId());
    }

    private Match persistMatch(String reservationId, LocalDate matchDate, MatchStatus status) {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId(reservationId)
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .capacity(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .matchDate(matchDate)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .recruitDeadline(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(2))
                .participantCancelDeadline(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(1))
                .hostCancelDeadline(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(1))
                .build());
        if (status == MatchStatus.CONFIRMED || status == MatchStatus.COMPLETED) {
            match.confirm(LocalDateTime.of(matchDate, LocalTime.of(9, 0)));
        }
        if (status == MatchStatus.COMPLETED) {
            match.complete();
        }
        return matchRepository.save(match);
    }

    private void persistSettlement(String matchId) {
        settlementRepository.save(Settlement.create(
                matchId, "host-id", SportType.FUTSAL, 30_000, new BigDecimal("0.0700")));
    }
}
