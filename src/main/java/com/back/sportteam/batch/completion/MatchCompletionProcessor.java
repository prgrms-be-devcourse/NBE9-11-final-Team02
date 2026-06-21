package com.back.sportteam.batch.completion;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchCompletionProcessor {

    private final MatchRepository matchRepository;

    @Transactional
    public void process(String matchId) {
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != MatchStatus.CONFIRMED) {
            return;
        }
        match.complete();
    }
}
