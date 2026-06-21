package com.back.sportteam.domain.facility.controller;

import com.back.sportteam.domain.facility.service.FacilityService;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
                .build();
    }

    @Test
    void 시설명이_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/manager/facilities")
                        .header("X-USER-ID", "manager-id")
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
                        .header("X-USER-ID", "manager-id")
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
    void X_USER_ID_헤더가_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/manager/facilities"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 슬롯_요금이_음수이면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/manager/facilities/facility-id/slots")
                        .header("X-USER-ID", "manager-id")
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
                        .header("X-USER-ID", "manager-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": 10000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
