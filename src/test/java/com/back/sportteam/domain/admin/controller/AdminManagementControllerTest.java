package com.back.sportteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.domain.admin.service.AdminManagementService;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

class AdminManagementControllerTest {

    private AdminManagementService adminManagementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminManagementService = mock(AdminManagementService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminManagementController(adminManagementService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 전체_시설_목록을_페이지로_조회한다() throws Exception {
        when(adminManagementService.getFacilities(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/facilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void 전체_회원_목록을_페이지로_조회한다() throws Exception {
        when(adminManagementService.getUsers(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void 존재하지_않는_시설_상태를_요청하면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/facilities")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}
