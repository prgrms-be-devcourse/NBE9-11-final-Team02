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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettlementRepositoryQueryTest {

    private static final LocalDate TODAY = LocalDate.of(2099, Month.JUNE, 15);

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Test
    void 오늘_이전에_완료된_미정산_경기만_조회한다() {
        Match yesterday = saveCompleted(TODAY.minusDays(1));
        Match twoDaysAgo = saveCompleted(TODAY.minusDays(2));
        Match todayMatch = saveCompleted(TODAY);

        List<String> ids = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PageRequest.of(0, 100)
        );

        assertThat(ids)
                .containsExactlyInAnyOrder(yesterday.getId(), twoDaysAgo.getId())
                .doesNotContain(todayMatch.getId());
    }

    @Test
    void 이미_정산된_경기는_조회되지_않는다() {
        Match settled = saveCompleted(TODAY.minusDays(1));
        Match unsettled = saveCompleted(TODAY.minusDays(1));
        settlementRepository.save(Settlement.create(
                settled.getId(), "host-id", SportType.FUTSAL, 30_000, new BigDecimal("0.0700")
        ));

        List<String> ids = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PageRequest.of(0, 100)
        );

        assertThat(ids)
                .containsExactly(unsettled.getId())
                .doesNotContain(settled.getId());
    }

    @Test
    void 완료_상태가_아닌_경기는_조회되지_않는다() {
        Match completed = saveCompleted(TODAY.minusDays(1));
        Match confirmed = matchRepository.save(buildMatch(TODAY.minusDays(1)));
        confirmed.confirm(LocalDateTime.now());
        matchRepository.save(confirmed);

        List<String> ids = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, TODAY, PageRequest.of(0, 100)
        );

        assertThat(ids)
                .containsExactly(completed.getId())
                .doesNotContain(confirmed.getId());
    }

    private Match saveCompleted(LocalDate matchDate) {
        Match match = buildMatch(matchDate);
        match.confirm(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(1));
        match.complete();
        return matchRepository.save(match);
    }

    private Match buildMatch(LocalDate matchDate) {
        return Match.create(MatchCreateCommand.builder()
                .reservationId(UUID.randomUUID().toString())
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
                .cancelDeadline(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(1))
                .build());
    }
}
