package com.back.sportteam.domain.mypage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.MyMatchStatus;
import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import com.back.sportteam.domain.mypage.dto.response.MyRecordResponse;
import com.back.sportteam.domain.mypage.service.MyPageMatchService;
import com.back.sportteam.domain.mypage.service.MyPagePaymentService;
import com.back.sportteam.domain.mypage.service.MyPageRecordService;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;

class MyPageControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final LocalDate MATCH_DATE = LocalDate.of(2099, Month.JUNE, 10);
    private static final LocalTime START_TIME = LocalTime.of(10, 0);
    private static final LocalTime END_TIME = LocalTime.of(12, 0);

    private MyPageMatchService myPageMatchService;
    private MyPagePaymentService myPagePaymentService;
    private MyPageRecordService myPageRecordService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        myPageMatchService = mock(MyPageMatchService.class);
        myPagePaymentService = mock(MyPagePaymentService.class);
        myPageRecordService = mock(MyPageRecordService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MyPageController(myPageMatchService, myPagePaymentService, myPageRecordService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 필터_없이_요청해도_200을_반환한다() throws Exception {
        when(myPageMatchService.getMyMatches(eq(USER_ID), any(MyMatchCondition.class)))
                .thenReturn(new PageImpl<>(List.of(createResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].matchId").value("match-id"));

        verify(myPageMatchService).getMyMatches(eq(USER_ID), any(MyMatchCondition.class));
    }

    @Test
    void 필터_파라미터가_서비스에_올바르게_전달된다() throws Exception {
        when(myPageMatchService.getMyMatches(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        ArgumentCaptor<MyMatchCondition> captor = ArgumentCaptor.forClass(MyMatchCondition.class);

        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null))
                        .param("sportType", "FUTSAL")
                        .param("myMatchStatus", "PARTICIPATING")
                        .param("role", "HOST")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(myPageMatchService).getMyMatches(eq(USER_ID), captor.capture());
        MyMatchCondition condition = captor.getValue();
        assertThat(condition.sportType()).isEqualTo(SportType.FUTSAL);
        assertThat(condition.myMatchStatus()).isEqualTo(MyMatchStatus.PARTICIPATING);
        assertThat(condition.role()).isEqualTo(MatchParticipantRole.HOST);
        assertThat(condition.page()).isEqualTo(2);
        assertThat(condition.size()).isEqualTo(5);
    }

    @Test
    void 잘못된_sportType_값이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null))
                        .param("sportType", "INVALID_SPORT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 잘못된_role_값이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null))
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 잘못된_myMatchStatus_값이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null))
                        .param("myMatchStatus", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void page_0은_경계값으로_정상_처리된다() throws Exception {
        when(myPageMatchService.getMyMatches(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null))
                        .param("page", "0"))
                .andExpect(status().isOk());
    }

    @Test
    void size_1은_경계값으로_정상_처리된다() throws Exception {
        when(myPageMatchService.getMyMatches(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null))
                        .param("size", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void size_100은_경계값으로_정상_처리된다() throws Exception {
        when(myPageMatchService.getMyMatches(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get("/api/v1/users/me/matches")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null))
                        .param("size", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void 운동기록_요청시_200을_반환한다() throws Exception {
        when(myPageRecordService.getMyRecord(USER_ID)).thenReturn(recordResponse());

        mockMvc.perform(get("/api/v1/users/me/records")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMatchCount").value(10))
                .andExpect(jsonPath("$.data.mannerStat.mannerReviewCount").value(5));

        verify(myPageRecordService).getMyRecord(USER_ID);
    }

    @Test
    void 운동기록_조회시_존재하지_않는_유저면_404를_반환한다() throws Exception {
        when(myPageRecordService.getMyRecord(USER_ID))
                .thenThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/me/records")
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 운동기록_조회시_비로그인_상태면_userId가_null로_전달된다() throws Exception {
        when(myPageRecordService.getMyRecord(null)).thenReturn(recordResponse());

        mockMvc.perform(get("/api/v1/users/me/records"))
                .andExpect(status().isOk());

        verify(myPageRecordService).getMyRecord(null);
    }

    private MyRecordResponse recordResponse() {
        return new MyRecordResponse(
                10,
                3,
                7,
                List.of(new MyRecordResponse.SportStat(SportType.FUTSAL, 10L)),
                List.of(new MyRecordResponse.MonthlyStat(2026, 4, 3L)),
                new MyRecordResponse.MannerStat(new BigDecimal("4.50"), 5),
                List.of()
        );
    }

    private MyMatchResponse createResponse() {
        return new MyMatchResponse(
                "match-id",
                "풋살 매칭",
                SportType.FUTSAL,
                MyMatchStatus.PARTICIPATING,
                MatchParticipantRole.HOST,
                MATCH_DATE,
                START_TIME,
                END_TIME
        );
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
}
