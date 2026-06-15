package com.back.sportteam.batch.cancel;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.global.util.TimeUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchDeadlineScheduler {

    private final MatchRepository matchRepository;
    private final MatchDeadlineProcessor matchDeadlineProcessor;

    @Value("${app.scheduler.match-deadline.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.scheduler.match-deadline.fixed-delay-ms:60000}")
    public void processExpiredMatches() {
        LocalDateTime processedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
        List<String> matchIds = matchRepository.findIdsByStatusAndRecruitDeadlineBefore(
                MatchStatus.RECRUITING,
                processedAt,
                PageRequest.of(0, batchSize)
        );

        for (String matchId : matchIds) {
            processMatch(matchId, processedAt);
        }
    }

    private void processMatch(String matchId, LocalDateTime processedAt) {
        try {
            matchDeadlineProcessor.process(matchId, processedAt);
        } catch (RuntimeException e) {
            log.error("Failed to process expired match. matchId={}", matchId, e);
        }
    }
}
