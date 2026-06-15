package com.back.sportteam.domain.facility.controller;

import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.service.FacilityService;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FacilityUserControllerTest {

    private FacilityService facilityService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        facilityService = mock(FacilityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityUserController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 시설_상세_정보를_조회하면_200_응답을_반환한다() throws Exception {
        FacilityResponse response = new FacilityResponse(
                "facility-id", "테스트 풋살장", "서울시 강남구", "02-1234-5678",
                "설명", 10, 60, 10000, 15000, null,
                FacilityStatus.ACTIVE, Set.of(), Set.of(), null, null
        );
        when(facilityService.getFacility("facility-id")).thenReturn(response);

        mockMvc.perform(get("/api/v1/facilities/facility-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("facility-id"))
                .andExpect(jsonPath("$.data.name").value("테스트 풋살장"));
    }

    @Test
    void 날짜_파라미터가_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/facility-id/slots"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 날짜_형식이_올바르지_않으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/facility-id/slots")
                        .param("date", "2026-07-99"))
                .andExpect(status().isBadRequest());
    }
}
