package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchJoinFacadeTest {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, Month.JUNE, 16, 9, 0);

    @Test
    void 분산락_참가_요청을_서비스로_위임한다() {
        MatchService matchService = mock(MatchService.class);
        MatchJoinFacade facade = new MatchJoinFacade(matchService);
        MatchParticipantResponse participantResponse = new MatchParticipantResponse(
                "participant-id",
                "user-id",
                "user-nickname",
                MatchParticipantRole.PARTICIPANT,
                MatchParticipantStatus.ACTIVE,
                JOINED_AT
        );
        when(matchService.joinMatch("match-id", "user-id")).thenReturn(participantResponse);

        MatchParticipantResponse response = facade.joinMatchWithDistributedLock("match-id", "user-id");

        assertThat(response).isEqualTo(participantResponse);
        verify(matchService).joinMatch("match-id", "user-id");
    }
}
