package com.back.sportteam.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.domain.admin.dto.response.AdminUserResponse;
import com.back.sportteam.domain.admin.service.AdminManagementService;
import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
    void getFacilitiesReturnsPagedResponse() throws Exception {
        when(adminManagementService.getFacilities(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/facilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getUsersReturnsPagedResponse() throws Exception {
        when(adminManagementService.getUsers(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void invalidFacilityStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/facilities")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRestrictedUsersReturnsPagedResponse() throws Exception {
        when(adminManagementService.getRestrictedUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/users/blacklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getBlacklistCandidatesReturnsPagedResponse() throws Exception {
        when(adminManagementService.getBlacklistCandidates(any(BigDecimal.class), eq(5), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/users/blacklist/candidates")
                        .param("maxMannerScore", "2.00")
                        .param("minReviewCount", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void updateUserRestrictionReturnsUpdatedUser() throws Exception {
        AdminUserResponse response = new AdminUserResponse(
                "user-id",
                "user@example.com",
                "user",
                UserRole.USER,
                AuthProvider.LOCAL,
                "Seoul",
                new BigDecimal("2.00"),
                new BigDecimal("3.00"),
                5,
                true,
                "bad manner",
                LocalDateTime.of(2026, Month.JUNE, 24, 12, 0),
                LocalDateTime.of(2026, Month.JUNE, 24, 10, 0)
        );
        when(adminManagementService.updateUserRestriction(eq("user-id"), eq(true), eq("bad manner")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/users/user-id/restriction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restricted": true,
                                  "reason": "bad manner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user-id"))
                .andExpect(jsonPath("$.data.restricted").value(true))
                .andExpect(jsonPath("$.data.restrictionReason").value("bad manner"));
    }
}
