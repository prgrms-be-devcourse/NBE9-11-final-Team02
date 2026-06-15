package com.back.sportteam.batch.cancel;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.repository.MatchRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchDeadlineSchedulerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchDeadlineProcessor matchDeadlineProcessor;

    private MatchDeadlineScheduler matchDeadlineScheduler;

    @BeforeEach
    void setUp() {
        matchDeadlineScheduler = new MatchDeadlineScheduler(matchRepository, matchDeadlineProcessor);
        ReflectionTestUtils.setField(matchDeadlineScheduler, "batchSize", 100);
    }

    @Test
    void 한_경기_처리가_실패해도_다음_경기를_계속_처리한다() {
        when(matchRepository.findIdsByStatusAndRecruitDeadlineBefore(
                eq(MatchStatus.RECRUITING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of("match-1", "match-2"));
        doThrow(new IllegalStateException("processing failed"))
                .when(matchDeadlineProcessor)
                .process(eq("match-1"), any(LocalDateTime.class));

        matchDeadlineScheduler.processExpiredMatches();

        verify(matchDeadlineProcessor)
                .process(eq("match-2"), any(LocalDateTime.class));
    }
}
