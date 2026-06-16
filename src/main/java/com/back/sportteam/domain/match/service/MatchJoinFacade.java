package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.global.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchJoinFacade {

    // 매칭방별로 락을 분리해 서로 다른 매칭방 참가 요청은 막지 않는다.
    private static final String JOIN_LOCK_KEY = "'match:join:' + #matchId";

    private final MatchService matchService;

    // Facade에서 락을 먼저 획득하고, Service의 트랜잭션 로직을 호출한다.
    @DistributedLock(key = JOIN_LOCK_KEY, waitTime = 3L, leaseTime = 5L)
    public MatchParticipantResponse joinMatchWithDistributedLock(String matchId, String userId) {
        return matchService.joinMatch(matchId, userId);
    }
}
