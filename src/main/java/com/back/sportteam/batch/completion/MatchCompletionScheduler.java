package com.back.sportteam.batch.completion;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.global.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchCompletionScheduler {

    private final MatchRepository matchRepository;
    private final MatchCompletionProcessor matchCompletionProcessor;

    @Value("${app.scheduler.match-completion.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.scheduler.match-completion.fixed-delay-ms:600000}")
    public void completeEndedMatches() {
        LocalDateTime now = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
        List<String> matchIds = matchRepository.findIdsByStatusAndEndedBefore(
                MatchStatus.CONFIRMED,
                now.toLocalDate(),
                now.toLocalTime(),
                PageRequest.of(0, batchSize)
        );

        for (String matchId : matchIds) {
            completeMatch(matchId);
        }
    }

    private void completeMatch(String matchId) {
        try {
            matchCompletionProcessor.process(matchId);
        } catch (RuntimeException e) {
            log.error("Failed to complete match. matchId={}", matchId, e);
        }
    }
}
