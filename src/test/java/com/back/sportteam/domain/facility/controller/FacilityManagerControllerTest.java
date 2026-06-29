package com.back.sportteam.domain.facility.controller;

import com.back.sportteam.domain.facility.service.FacilityService;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FacilityManagerControllerTest {

    private FacilityService facilityService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        facilityService = mock(FacilityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityManagerController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticationPrincipalResolver())
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
    void 시설명이_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/manager/facilities")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "address": "서울시 강남구",
                                  "capacity": 10,
                                  "slotDurationMinutes": 60,
                                  "defaultWeekdayPrice": 10000,
                                  "defaultWeekendPrice": 15000,
                                  "sportTypes": ["FUTSAL"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 수용_인원이_1보다_작으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/manager/facilities")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "테스트 풋살장",
                                  "address": "서울시 강남구",
                                  "capacity": 0,
                                  "slotDurationMinutes": 60,
                                  "defaultWeekdayPrice": 10000,
                                  "defaultWeekendPrice": 15000,
                                  "sportTypes": ["FUTSAL"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증_사용자가_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/manager/facilities"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 시설_예약_현황을_조회하면_200_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/manager/facilities/facility-id/reservations")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null))
                        .param("fromDate", "2026-07-01")
                        .param("toDate", "2026-07-31"))
                .andExpect(status().isOk());
    }

    @Test
    void 시설_예약_조회_날짜가_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/manager/facilities/facility-id/reservations")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 슬롯_요금이_음수이면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/manager/facilities/facility-id/slots")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromDate": "2026-07-01",
                                  "toDate": "2026-07-07",
                                  "startTime": "09:00:00",
                                  "endTime": "21:00:00",
                                  "weekdayPrice": -1000,
                                  "weekendPrice": 15000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 슬롯_상태가_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/manager/facilities/facility-id/slots/slot-id")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 10000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void imageUrl_없이_이미지_삭제_요청하면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/manager/facilities/facility-id/images")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 이미지_삭제_요청이_정상이면_200_응답을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/manager/facilities/facility-id/images")
                        .principal(new UsernamePasswordAuthenticationToken("manager-id", null))
                        .param("imageUrl", "https://bucket.s3.ap-northeast-2.amazonaws.com/facilities/uuid"))
                .andExpect(status().isOk());
    }
}
