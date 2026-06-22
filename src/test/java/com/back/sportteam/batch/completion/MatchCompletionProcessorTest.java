package com.back.sportteam.batch.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchCompletionProcessorTest {

    private static final LocalDate MATCH_DATE = LocalDate.of(2099, Month.JUNE, 10);
    private static final LocalTime MATCH_START_TIME = LocalTime.of(10, 0);
    private static final LocalTime MATCH_END_TIME = LocalTime.of(12, 0);

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchCompletionProcessor matchCompletionProcessor;

    @Test
    void 처리_대상_경기가_확정_상태면_완료로_전이한다() {
        Match match = createMatch();
        match.confirm(LocalDateTime.now());
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        matchCompletionProcessor.process(match.getId());

        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
    }

    @Test
    void 처리_대상_경기가_확정_상태가_아니면_완료_처리하지_않는다() {
        Match match = createMatch();
        match.cancel(LocalDateTime.now());
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        matchCompletionProcessor.process(match.getId());

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELLED);
    }

    @Test
    void 처리_대상_경기가_존재하지_않으면_예외없이_무시한다() {
        when(matchRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatNoException()
                .isThrownBy(() -> matchCompletionProcessor.process("unknown"));
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
