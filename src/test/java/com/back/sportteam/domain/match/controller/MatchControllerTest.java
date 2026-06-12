package com.back.sportteam.domain.match.controller;

import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.dto.response.MatchDetailResponse;
import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.dto.response.MatchSummaryResponse;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.service.MatchService;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchControllerTest {

    private static final LocalDateTime CANCEL_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 12, 10, 0);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.JUNE, 11, 10, 0);

    private MatchService matchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        matchService = mock(MatchService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MatchController(matchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 매칭방_생성_요청을_201_응답으로_반환한다() throws Exception {
        MatchCreateRequest request = createRequest("풋살 매칭");
        MatchCreateResponse response = createResponse(request);
        when(matchService.createMatch(eq("host-id"), any(MatchCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/matches")
                        .header("X-USER-ID", "host-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchId").value("match-id"))
                .andExpect(jsonPath("$.data.currentCount").value(1))
                .andExpect(jsonPath("$.data.status").value("RECRUITING"));

        verify(matchService).createMatch(eq("host-id"), any(MatchCreateRequest.class));
    }

    @Test
    void 매칭방_제목이_비어있으면_400_응답으로_반환한다() throws Exception {
        MatchCreateRequest request = createRequest("");

        mockMvc.perform(post("/api/v1/matches")
                        .header("X-USER-ID", "host-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_002"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/matches"));
    }

    @Test
    void 매칭방_목록을_200_응답으로_반환한다() throws Exception {
        when(matchService.getMatches()).thenReturn(List.of(createSummaryResponse()));

        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].matchId").value("match-id"))
                .andExpect(jsonPath("$.data[0].title").value("풋살 매칭"))
                .andExpect(jsonPath("$.data[0].feePerPerson").value(10000))
                .andExpect(jsonPath("$.data[0].status").value("RECRUITING"));

        verify(matchService).getMatches();
    }

    @Test
    void 매칭방_단건을_200_응답으로_반환한다() throws Exception {
        when(matchService.getMatch("match-id")).thenReturn(createDetailResponse());

        mockMvc.perform(get("/api/v1/matches/{matchId}", "match-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchId").value("match-id"))
                .andExpect(jsonPath("$.data.reservationId").value("reservation-id"))
                .andExpect(jsonPath("$.data.hostId").value("host-id"))
                .andExpect(jsonPath("$.data.title").value("풋살 매칭"))
                .andExpect(jsonPath("$.data.status").value("RECRUITING"));

        verify(matchService).getMatch("match-id");
    }

    @Test
    void 매칭방_단건이_없으면_404_응답으로_반환한다() throws Exception {
        when(matchService.getMatch("missing-id")).thenThrow(new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        mockMvc.perform(get("/api/v1/matches/{matchId}", "missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_001"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/matches/missing-id"));
    }

    @Test
    void 매칭방_참가자_목록을_200_응답으로_반환한다() throws Exception {
        when(matchService.getParticipants("match-id")).thenReturn(List.of(createParticipantResponse()));

        mockMvc.perform(get("/api/v1/matches/{matchId}/participants", "match-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].participantId").value("participant-id"))
                .andExpect(jsonPath("$.data[0].userId").value("host-id"))
                .andExpect(jsonPath("$.data[0].role").value("HOST"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        verify(matchService).getParticipants("match-id");
    }

    @Test
    void 매칭방_참가자_목록_조회시_매칭방이_없으면_404_응답으로_반환한다() throws Exception {
        when(matchService.getParticipants("missing-id")).thenThrow(new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        mockMvc.perform(get("/api/v1/matches/{matchId}/participants", "missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_001"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/matches/missing-id/participants"));
    }

    private MatchCreateRequest createRequest(String title) {
        return new MatchCreateRequest(
                "reservation-id",
                title,
                SportType.FUTSAL,
                2,
                10,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                CANCEL_DEADLINE
        );
    }

    private MatchCreateResponse createResponse(MatchCreateRequest request) {
        return new MatchCreateResponse(
                "match-id",
                request.reservationId(),
                "host-id",
                request.title(),
                request.sportType(),
                request.minParticipants(),
                request.maxParticipants(),
                1,
                request.feePerPerson(),
                request.minSkillLevel(),
                request.maxSkillLevel(),
                request.requiredGender(),
                request.cancelDeadline(),
                MatchStatus.RECRUITING,
                CREATED_AT
        );
    }

    private MatchSummaryResponse createSummaryResponse() {
        return new MatchSummaryResponse(
                "match-id",
                "풋살 매칭",
                SportType.FUTSAL,
                1,
                10,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                MatchStatus.RECRUITING
        );
    }

    private MatchDetailResponse createDetailResponse() {
        return new MatchDetailResponse(
                "match-id",
                "reservation-id",
                "host-id",
                "풋살 매칭",
                SportType.FUTSAL,
                2,
                10,
                1,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                CANCEL_DEADLINE,
                MatchStatus.RECRUITING,
                CREATED_AT,
                CREATED_AT
        );
    }

    private MatchParticipantResponse createParticipantResponse() {
        return new MatchParticipantResponse(
                "participant-id",
                "host-id",
                MatchParticipantRole.HOST,
                MatchParticipantStatus.ACTIVE,
                CREATED_AT
        );
    }

    private String json(MatchCreateRequest request) {
        return """
                {
                  "reservationId": "%s",
                  "title": "%s",
                  "sportType": "%s",
                  "minParticipants": %d,
                  "maxParticipants": %d,
                  "feePerPerson": %d,
                  "minSkillLevel": "%s",
                  "maxSkillLevel": "%s",
                  "requiredGender": "%s",
                  "cancelDeadline": "%s"
                }
                """.formatted(
                request.reservationId(),
                request.title(),
                request.sportType(),
                request.minParticipants(),
                request.maxParticipants(),
                request.feePerPerson(),
                request.minSkillLevel(),
                request.maxSkillLevel(),
                request.requiredGender(),
                request.cancelDeadline()
        );
    }
}
