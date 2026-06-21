package com.back.sportteam.domain.match.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
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
class MatchCompletionQueryTest {

    private static final LocalDateTime REFERENCE_NOW =
            LocalDateTime.of(2099, Month.JUNE, 15, 15, 0);

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void 종료시각이_지난_확정경기만_조회한다() {
        Match yesterday = saveConfirmed(LocalDate.of(2099, Month.JUNE, 14), LocalTime.of(20, 0));
        Match endedToday = saveConfirmed(REFERENCE_NOW.toLocalDate(), LocalTime.of(14, 0));
        Match endedExactly = saveConfirmed(REFERENCE_NOW.toLocalDate(), REFERENCE_NOW.toLocalTime());
        Match stillRunning = saveConfirmed(REFERENCE_NOW.toLocalDate(), LocalTime.of(16, 0));

        List<String> ids = matchRepository.findIdsByStatusAndEndedBefore(
                MatchStatus.CONFIRMED,
                REFERENCE_NOW.toLocalDate(),
                REFERENCE_NOW.toLocalTime(),
                PageRequest.of(0, 100)
        );

        assertThat(ids)
                .containsExactlyInAnyOrder(yesterday.getId(), endedToday.getId(), endedExactly.getId())
                .doesNotContain(stillRunning.getId());
    }

    @Test
    void 확정_상태가_아닌_경기는_종료됐어도_조회되지_않는다() {
        Match confirmed = saveConfirmed(LocalDate.of(2099, Month.JUNE, 14), LocalTime.of(20, 0));
        Match recruiting = matchRepository.save(buildMatch(LocalDate.of(2099, Month.JUNE, 14), LocalTime.of(20, 0)));

        List<String> ids = matchRepository.findIdsByStatusAndEndedBefore(
                MatchStatus.CONFIRMED,
                REFERENCE_NOW.toLocalDate(),
                REFERENCE_NOW.toLocalTime(),
                PageRequest.of(0, 100)
        );

        assertThat(ids)
                .containsExactly(confirmed.getId())
                .doesNotContain(recruiting.getId());
    }

    private Match saveConfirmed(LocalDate matchDate, LocalTime endTime) {
        Match match = buildMatch(matchDate, endTime);
        match.confirm(LocalDateTime.of(matchDate, endTime).minusDays(1));
        return matchRepository.save(match);
    }

    private Match buildMatch(LocalDate matchDate, LocalTime endTime) {
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
                .startTime(endTime.minusHours(2))
                .endTime(endTime)
                .recruitDeadline(LocalDateTime.of(matchDate, endTime).minusDays(2))
                .cancelDeadline(LocalDateTime.of(matchDate, endTime).minusDays(1))
                .build());
    }
}
