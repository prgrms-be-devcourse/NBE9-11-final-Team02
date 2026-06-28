package com.back.sportteam.batch.settlement;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import com.back.sportteam.global.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SettlementSchedulerProperties properties;

    @Scheduled(cron = "${app.scheduler.settlement.cron:0 0 2 * * *}", zone = "Asia/Seoul")
    public void settleCompletedMatches() {
        LocalDate today = LocalDate.now(TimeUtils.SERVICE_ZONE);

        List<String> matchIds = settlementRepository.findUnsettledCompletedMatchIds(
                MatchStatus.COMPLETED, today, PageRequest.of(0, properties.batchSize()));
        int processed = 0;
        for (int from = 0; from < matchIds.size(); from += properties.chunkSize()) {
            List<String> chunk = matchIds.subList(from, Math.min(from + properties.chunkSize(), matchIds.size()));
            try {
                settlementProcessor.processBatch(chunk);
                processed += chunk.size();
            } catch (RuntimeException e) {
                log.error("[Settlement] 청크 처리 실패. matchIds={}", chunk, e);
            }
        }

        if (!matchIds.isEmpty()) {
            log.info("[Settlement] 배치 완료. processed={}건", processed);
        }
    }
}
