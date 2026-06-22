package com.back.sportteam.batch.completion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.repository.MatchRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchCompletionSchedulerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchCompletionProcessor matchCompletionProcessor;

    private MatchCompletionScheduler matchCompletionScheduler;

    @BeforeEach
    void setUp() {
        matchCompletionScheduler = new MatchCompletionScheduler(matchRepository, matchCompletionProcessor);
        ReflectionTestUtils.setField(matchCompletionScheduler, "batchSize", 100);
    }

    @Test
    void 한_경기_처리가_실패해도_다음_경기를_계속_처리한다() {
        when(matchRepository.findIdsByStatusAndEndedBefore(
                eq(MatchStatus.CONFIRMED),
                any(LocalDate.class),
                any(LocalTime.class),
                any(Pageable.class)
        )).thenReturn(List.of("match-1", "match-2"));
        doThrow(new IllegalStateException("processing failed"))
                .when(matchCompletionProcessor)
                .process("match-1");

        matchCompletionScheduler.completeEndedMatches();

        verify(matchCompletionProcessor).process("match-2");
    }
}
