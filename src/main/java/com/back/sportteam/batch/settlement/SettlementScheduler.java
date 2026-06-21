package com.back.sportteam.batch.settlement;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import com.back.sportteam.global.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementRepository settlementRepository;
    private final SettlementProcessor settlementProcessor;

    @Value("${app.scheduler.settlement.batch-size:100}")
    private int batchSize;

    @Scheduled(cron = "${app.scheduler.settlement.cron:0 0 2 * * *}", zone = "Asia/Seoul")
    public void settleCompletedMatches() {
        LocalDate today = LocalDate.now(TimeUtils.SERVICE_ZONE);
        List<String> matchIds = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED,
                today,
                PageRequest.of(0, batchSize)
        );

        for (String matchId : matchIds) {
            settle(matchId);
        }
    }

    private void settle(String matchId) {
        try {
            settlementProcessor.process(matchId);
        } catch (RuntimeException e) {
            log.error("Failed to settle match. matchId={}", matchId, e);
        }
    }
}
