package com.back.sportteam.domain.match.controller;

import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.service.MatchService;
import com.back.sportteam.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchLockTestControllerTest {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, Month.JUNE, 16, 9, 0);

    @Test
    void 비관적락_비교용_참가_요청을_201_응답으로_반환한다() {
        MatchService matchService = mock(MatchService.class);
        MatchLockTestController controller = new MatchLockTestController(matchService);
        MatchParticipantResponse participantResponse = new MatchParticipantResponse(
                "participant-id",
                "user-id",
                MatchParticipantRole.PARTICIPANT,
                MatchParticipantStatus.ACTIVE,
                JOINED_AT
        );
        when(matchService.joinMatchWithPessimisticLock("match-id", "user-id")).thenReturn(participantResponse);

        ResponseEntity<ApiResponse<MatchParticipantResponse>> response =
                controller.joinMatchWithPessimisticLock("match-id", "user-id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(participantResponse);
        verify(matchService).joinMatchWithPessimisticLock("match-id", "user-id");
    }
}
