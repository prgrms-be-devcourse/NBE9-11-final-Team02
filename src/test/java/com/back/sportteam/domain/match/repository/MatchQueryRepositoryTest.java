package com.back.sportteam.domain.match.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.match.dto.request.MatchSearchCondition;
import com.back.sportteam.domain.match.dto.request.MatchSortType;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.global.config.QueryDslConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({QueryDslConfig.class, MatchQueryRepository.class})
@ActiveProfiles("test")
class MatchQueryRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, Month.JUNE, 23, 10, 0);

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchQueryRepository matchQueryRepository;

    @BeforeEach
    void setUp() {
        matchRepository.save(createMatch(
                "reservation-futsal",
                "풋살 매칭",
                SportType.FUTSAL,
                20_000,
                NOW.plusDays(1)
        ));
        matchRepository.save(createMatch(
                "reservation-tennis",
                "테니스 매칭",
                SportType.TENNIS,
                10_000,
                NOW.plusDays(2)
        ));
    }

    @Test
    void 동적_조건으로_종목과_상태를_필터링한다() {
        MatchSearchCondition condition = condition(
                SportType.FUTSAL,
                MatchStatus.RECRUITING,
                MatchSortType.LATEST
        );

        Page<Match> result = matchQueryRepository.findAll(condition);

        assertThat(result.getContent())
                .extracting(Match::getTitle)
                .containsExactly("풋살 매칭");
    }

    @Test
    void 요금_오름차순으로_정렬한다() {
        MatchSearchCondition condition = condition(null, null, MatchSortType.FEE_ASC);

        Page<Match> result = matchQueryRepository.findAll(condition);

        assertThat(result.getContent())
                .extracting(Match::getFeePerPerson)
                .containsExactly(10_000, 20_000);
    }

    @Test
    void 추천_후보는_모집중이고_마감되지_않은_빈자리_매칭만_조회한다() {
        Match fullMatch = createMatch(
                "reservation-full",
                "정원 마감 매칭",
                SportType.FUTSAL,
                30_000,
                NOW.plusDays(1)
        );
        for (int i = 0; i < fullMatch.getCapacity() - 1; i++) {
            fullMatch.increaseCurrentCount();
        }
        matchRepository.save(fullMatch);
        matchRepository.save(createMatch(
                "reservation-expired",
                "모집 종료 매칭",
                SportType.FUTSAL,
                40_000,
                NOW.minusMinutes(1)
        ));

        List<Match> result = matchQueryRepository.findRecommendationCandidates(
                SportType.FUTSAL,
                MatchStatus.RECRUITING,
                NOW,
                10
        );

        assertThat(result)
                .extracting(Match::getTitle)
                .containsExactly("풋살 매칭");
    }

    private MatchSearchCondition condition(
            SportType sportType,
            MatchStatus status,
            MatchSortType sortType
    ) {
        return new MatchSearchCondition(
                sportType,
                status,
                null,
                null,
                null,
                sortType,
                0,
                20
        );
    }

    private Match createMatch(
            String reservationId,
            String title,
            SportType sportType,
            int feePerPerson,
            LocalDateTime recruitDeadline
    ) {
        return Match.create(MatchCreateCommand.builder()
                .reservationId(reservationId)
                .hostId("host-id")
                .title(title)
                .sportType(sportType)
                .capacity(2)
                .feePerPerson(feePerPerson)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .matchDate(LocalDate.of(2099, Month.JUNE, 30))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .recruitDeadline(recruitDeadline)
                .participantCancelDeadline(recruitDeadline.plusHours(1))

                .hostCancelDeadline(recruitDeadline.plusHours(1))
                .build());
    }
}
