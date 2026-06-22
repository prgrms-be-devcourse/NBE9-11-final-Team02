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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementProcessor settlementProcessor;

    private SettlementScheduler settlementScheduler;

    @BeforeEach
    void setUp() {
        settlementScheduler = new SettlementScheduler(settlementRepository, settlementProcessor);
        ReflectionTestUtils.setField(settlementScheduler, "batchSize", 1000);
    }

    @Test
    void 한_경기_정산이_실패해도_다음_경기_정산을_계속_처리한다() {
        when(settlementRepository.findUnsettledCompletedMatchIds(
                eq(MatchStatus.COMPLETED),
                any(LocalDate.class),
                any(Pageable.class)
        )).thenReturn(List.of("match-1", "match-2"));
        doThrow(new IllegalStateException("processing failed"))
                .when(settlementProcessor)
                .process("match-1");

        settlementScheduler.settleCompletedMatches();

        verify(settlementProcessor).process("match-2");
    }
}
