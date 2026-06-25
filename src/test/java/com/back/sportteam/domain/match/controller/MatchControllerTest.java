package com.back.sportteam.domain.match.controller;

import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.request.MatchRecommendationRequest;
import com.back.sportteam.domain.match.dto.request.MatchSearchCondition;
import com.back.sportteam.domain.match.dto.request.MatchSortType;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.dto.response.MatchDetailResponse;
import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.dto.response.MatchRecommendationResponse;
import com.back.sportteam.domain.match.dto.response.MatchSummaryResponse;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.service.MatchJoinFacade;
import com.back.sportteam.domain.match.service.MatchService;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchControllerTest {

    private static final LocalDateTime RECRUIT_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 10, 10, 0);
    private static final LocalDateTime PARTICIPANT_CANCEL_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 9, 10, 0);
    private static final LocalDateTime HOST_CANCEL_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 7, 10, 0);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.JUNE, 11, 10, 0);

    private MatchService matchService;
    private MatchJoinFacade matchJoinFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        matchService = mock(MatchService.class);
        matchJoinFacade = mock(MatchJoinFacade.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MatchController(matchService, matchJoinFacade))
                .setCustomArgumentResolvers(authenticationPrincipalResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                if (webRequest.getUserPrincipal() instanceof Authentication authentication) {
                    return authentication.getPrincipal();
                }
                return null;
            }
        };
    }

    @Test
    void 매칭방_생성_요청을_201_응답으로_반환한다() throws Exception {
        MatchCreateRequest request = createRequest("풋살 매칭");
        MatchCreateResponse response = createResponse(request);
        when(matchService.createMatch(eq("host-id"), any(MatchCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/matches")
                        .principal(new UsernamePasswordAuthenticationToken("host-id", null))
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
                        .principal(new UsernamePasswordAuthenticationToken("host-id", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_002"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/matches"));
    }

    @Test
    void 매칭방_목록을_200_응답으로_반환한다() throws Exception {
        when(matchService.getMatches(any(MatchSearchCondition.class)))
                .thenReturn(new PageImpl<>(List.of(createSummaryResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].matchId").value("match-id"))
                .andExpect(jsonPath("$.data.content[0].title").value("풋살 매칭"))
                .andExpect(jsonPath("$.data.content[0].feePerPerson").value(10000))
                .andExpect(jsonPath("$.data.content[0].status").value("RECRUITING"));

        verify(matchService).getMatches(any(MatchSearchCondition.class));
    }

    @Test
    void 매칭방_목록_조회시_필터와_페이징_조건을_전달한다() throws Exception {
        when(matchService.getMatches(any(MatchSearchCondition.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/v1/matches")
                        .param("sportType", "FUTSAL")
                        .param("status", "RECRUITING")
                        .param("minSkillLevel", "LEVEL_2")
                        .param("maxSkillLevel", "LEVEL_4")
                        .param("requiredGender", "MIXED")
                        .param("sort", "DEADLINE_ASC")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<MatchSearchCondition> conditionCaptor = forClass(MatchSearchCondition.class);
        verify(matchService).getMatches(conditionCaptor.capture());

        MatchSearchCondition condition = conditionCaptor.getValue();
        assertThat(condition.sportType()).isEqualTo(SportType.FUTSAL);
        assertThat(condition.status()).isEqualTo(MatchStatus.RECRUITING);
        assertThat(condition.minSkillLevel()).isEqualTo(SkillLevel.LEVEL_2);
        assertThat(condition.maxSkillLevel()).isEqualTo(SkillLevel.LEVEL_4);
        assertThat(condition.requiredGender()).isEqualTo(RequiredGender.MIXED);
        assertThat(condition.sort()).isEqualTo(MatchSortType.DEADLINE_ASC);
        assertThat(condition.page()).isEqualTo(1);
        assertThat(condition.size()).isEqualTo(5);
    }

    @Test
    void 매칭방_추천_목록을_200_응답으로_반환한다() throws Exception {
        when(matchService.recommendMatches(eq("user-id"), any(MatchRecommendationRequest.class)))
                .thenReturn(List.of(createRecommendationResponse()));

        mockMvc.perform(get("/api/v1/matches/recommendations")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null))
                        .param("sportType", "FUTSAL")
                        .param("gender", "MIXED")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].matchId").value("match-id"))
                .andExpect(jsonPath("$.data[0].recommendationScore").value(90))
                .andExpect(jsonPath("$.data[0].reasons[0]").value("실력 조건이 일치합니다."));

        ArgumentCaptor<MatchRecommendationRequest> requestCaptor = forClass(MatchRecommendationRequest.class);
        verify(matchService).recommendMatches(eq("user-id"), requestCaptor.capture());

        MatchRecommendationRequest request = requestCaptor.getValue();
        assertThat(request.sportType()).isEqualTo(SportType.FUTSAL);
        assertThat(request.gender()).isEqualTo(RequiredGender.MIXED);
        assertThat(request.recommendationSize()).isEqualTo(3);
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

    @Test
    void 매칭방_참가_요청을_201_응답으로_반환한다() throws Exception {
        when(matchJoinFacade.joinMatchWithDistributedLock("match-id", "user-id"))
                .thenReturn(createPendingParticipantResponse("participant-id", "user-id"));

        mockMvc.perform(post("/api/v1/matches/{matchId}/participants", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participantId").value("participant-id"))
                .andExpect(jsonPath("$.data.userId").value("user-id"))
                .andExpect(jsonPath("$.data.role").value("PARTICIPANT"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(matchJoinFacade).joinMatchWithDistributedLock("match-id", "user-id");
    }

    @Test
    void 매칭방_참가시_이미_참가한_유저면_409_응답으로_반환한다() throws Exception {
        when(matchJoinFacade.joinMatchWithDistributedLock("match-id", "user-id"))
                .thenThrow(new BusinessException(MatchErrorCode.ALREADY_PARTICIPATED));

        mockMvc.perform(post("/api/v1/matches/{matchId}/participants", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_003"));
    }

    @Test
    void 매칭방_참가_취소를_200_응답으로_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/matches/{matchId}/participants/me", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(matchService).leaveMatch("match-id", "user-id");
    }

    @Test
    void 매칭방_참가_취소시_참가정보가_없으면_404_응답으로_반환한다() throws Exception {
        doThrow(new BusinessException(MatchErrorCode.PARTICIPANT_NOT_FOUND))
                .when(matchService).leaveMatch("match-id", "user-id");

        mockMvc.perform(delete("/api/v1/matches/{matchId}/participants/me", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_009"));
    }

    @Test
    void 매칭방_참가_취소시_방장이면_400_응답으로_반환한다() throws Exception {
        doThrow(new BusinessException(MatchErrorCode.HOST_CANNOT_LEAVE))
                .when(matchService).leaveMatch("match-id", "host-id");

        mockMvc.perform(delete("/api/v1/matches/{matchId}/participants/me", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("host-id", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_010"));
    }

    @Test
    void 매칭방_참가_취소시_이탈_가능_시간이_지났으면_409_응답으로_반환한다() throws Exception {
        doThrow(new BusinessException(MatchErrorCode.LEAVE_DEADLINE_PASSED))
                .when(matchService).leaveMatch("match-id", "user-id");

        mockMvc.perform(delete("/api/v1/matches/{matchId}/participants/me", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_016"));
    }

    @Test
    void 매칭방_확정_요청을_200_응답으로_반환한다() throws Exception {
        MatchDetailResponse response = createConfirmedDetailResponse();
        when(matchService.confirmMatch("match-id", "host-id")).thenReturn(response);

        mockMvc.perform(patch("/api/v1/matches/{matchId}/confirm", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("host-id", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchId").value("match-id"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedAt").exists());

        verify(matchService).confirmMatch("match-id", "host-id");
    }

    @Test
    void 매칭방_확정시_방장이_아니면_403_응답으로_반환한다() throws Exception {
        when(matchService.confirmMatch("match-id", "user-id"))
                .thenThrow(new BusinessException(MatchErrorCode.NOT_MATCH_OWNER));

        mockMvc.perform(patch("/api/v1/matches/{matchId}/confirm", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_004"));
    }

    @Test
    void 매칭방_취소_요청을_200_응답으로_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/matches/{matchId}", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("host-id", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(matchService).cancelMatch("match-id", "host-id");
    }

    @Test
    void 매칭방_취소시_방장이_아니면_403_응답으로_반환한다() throws Exception {
        doThrow(new BusinessException(MatchErrorCode.NOT_MATCH_OWNER))
                .when(matchService).cancelMatch("match-id", "user-id");

        mockMvc.perform(delete("/api/v1/matches/{matchId}", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("user-id", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_004"));
    }

    @Test
    void 매칭방_취소시_취소_가능_시간이_지났으면_409_응답으로_반환한다() throws Exception {
        doThrow(new BusinessException(MatchErrorCode.CANCEL_DEADLINE_PASSED))
                .when(matchService).cancelMatch("match-id", "host-id");

        mockMvc.perform(delete("/api/v1/matches/{matchId}", "match-id")
                        .principal(new UsernamePasswordAuthenticationToken("host-id", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MATCH_015"));
    }

    private MatchCreateRequest createRequest(String title) {
        return new MatchCreateRequest(
                "reservation-id",
                title,
                SportType.FUTSAL,
                10,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                RECRUIT_DEADLINE,
                PARTICIPANT_CANCEL_DEADLINE,
                HOST_CANCEL_DEADLINE
        );
    }

    private MatchCreateResponse createResponse(MatchCreateRequest request) {
        return new MatchCreateResponse(
                "match-id",
                request.reservationId(),
                "host-id",
                request.title(),
                request.sportType(),
                request.capacity(),
                1,
                request.feePerPerson(),
                request.minSkillLevel(),
                request.maxSkillLevel(),
                request.requiredGender(),
                request.recruitDeadline(),
                request.participantCancelDeadline(),
                request.hostCancelDeadline(),
                null,
                null,
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
                RECRUIT_DEADLINE,
                MatchStatus.RECRUITING
        );
    }

    private MatchRecommendationResponse createRecommendationResponse() {
        return new MatchRecommendationResponse(
                "match-id",
                "풋살 매칭",
                SportType.FUTSAL,
                8,
                10,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                RECRUIT_DEADLINE,
                MatchStatus.RECRUITING,
                90,
                List.of("실력 조건이 일치합니다.")
        );
    }

    private MatchDetailResponse createDetailResponse() {
        return new MatchDetailResponse(
                "match-id",
                "reservation-id",
                "host-id",
                "풋살 매칭",
                SportType.FUTSAL,
                10,
                1,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                RECRUIT_DEADLINE,
                PARTICIPANT_CANCEL_DEADLINE,
                HOST_CANCEL_DEADLINE,
                null,
                null,
                MatchStatus.RECRUITING,
                CREATED_AT,
                CREATED_AT
        );
    }

    private MatchDetailResponse createConfirmedDetailResponse() {
        return new MatchDetailResponse(
                "match-id",
                "reservation-id",
                "host-id",
                "풋살 매칭",
                SportType.FUTSAL,
                10,
                1,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                RECRUIT_DEADLINE,
                PARTICIPANT_CANCEL_DEADLINE,
                HOST_CANCEL_DEADLINE,
                CREATED_AT,
                null,
                MatchStatus.CONFIRMED,
                CREATED_AT,
                CREATED_AT
        );
    }

    private MatchParticipantResponse createParticipantResponse() {
        return createParticipantResponse("participant-id", "host-id", MatchParticipantRole.HOST);
    }

    private MatchParticipantResponse createParticipantResponse(
            String participantId,
            String userId,
            MatchParticipantRole role
    ) {
        return createParticipantResponse(participantId, userId, role, MatchParticipantStatus.ACTIVE);
    }

    private MatchParticipantResponse createPendingParticipantResponse(String participantId, String userId) {
        return createParticipantResponse(
                participantId,
                userId,
                MatchParticipantRole.PARTICIPANT,
                MatchParticipantStatus.ACTIVE
        );
    }

    private MatchParticipantResponse createParticipantResponse(
            String participantId,
            String userId,
            MatchParticipantRole role,
            MatchParticipantStatus status
    ) {
        return new MatchParticipantResponse(
                participantId,
                userId,
                role,
                status,
                CREATED_AT
        );
    }

    private String json(MatchCreateRequest request) {
        return """
                {
                  "reservationId": "%s",
                  "title": "%s",
                  "sportType": "%s",
                  "capacity": %d,
                  "feePerPerson": %d,
                  "minSkillLevel": "%s",
                  "maxSkillLevel": "%s",
                  "requiredGender": "%s",
                  "recruitDeadline": "%s",
                  "participantCancelDeadline": "%s",
                  "hostCancelDeadline": "%s"
                }
                """.formatted(
                request.reservationId(),
                request.title(),
                request.sportType(),
                request.capacity(),
                request.feePerPerson(),
                request.minSkillLevel(),
                request.maxSkillLevel(),
                request.requiredGender(),
                request.recruitDeadline(),
                request.participantCancelDeadline(),
                request.hostCancelDeadline()
        );
    }
}
