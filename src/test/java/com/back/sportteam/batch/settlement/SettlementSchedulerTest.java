package com.back.sportteam.batch.settlement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementProcessor settlementProcessor;

    private SettlementScheduler settlementScheduler;

    @BeforeEach
    void setUp() {
        SettlementSchedulerProperties properties = new SettlementSchedulerProperties("0 0 2 * * *", 1000, 50);
        settlementScheduler = new SettlementScheduler(settlementRepository, settlementProcessor, properties);
    }

    @Test
    void 일부_경기_정산이_실패해도_나머지_경기를_계속_정산한다() {
        List<String> matchIds = IntStream.range(0, 60)
                .mapToObj(i -> "match-" + i)
                .toList();
        when(settlementRepository.findUnsettledCompletedMatchIds(
                eq(MatchStatus.COMPLETED), any(LocalDate.class), any(Pageable.class)
        )).thenReturn(matchIds);
        doThrow(new IllegalStateException("processing failed"))
                .when(settlementProcessor)
                .processBatch(matchIds.subList(0, 50));

        settlementScheduler.settleCompletedMatches();

        verify(settlementProcessor).processBatch(matchIds.subList(50, 60));
    }
}
