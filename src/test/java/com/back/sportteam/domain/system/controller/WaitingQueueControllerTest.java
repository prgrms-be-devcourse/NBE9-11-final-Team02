package com.back.sportteam.domain.system.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.domain.system.dto.response.WaitingQueueTokenResponse;
import com.back.sportteam.infra.redis.queue.WaitingQueueService;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WaitingQueueControllerTest {

    private WaitingQueueService waitingQueueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        waitingQueueService = mock(WaitingQueueService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WaitingQueueController(waitingQueueService))
                .build();
    }

    @Test
    void issueTokenReturnsCreatedResponse() throws Exception {
        when(waitingQueueService.issueToken("slot-id", "user-id"))
                .thenReturn(response(true));

        mockMvc.perform(post("/api/v1/queue/facility-slots/slot-id/tokens")
                        .header("X-USER-ID", "user-id"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("token-id"))
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.enterable").value(true));
    }

    @Test
    void getStatusReturnsQueueStatus() throws Exception {
        when(waitingQueueService.getStatus("token-id"))
                .thenReturn(response(false));

        mockMvc.perform(get("/api/v1/queue/tokens/token-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("token-id"))
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.enterable").value(false));
    }

    private WaitingQueueTokenResponse response(boolean enterable) {
        return new WaitingQueueTokenResponse(
                "token-id",
                "slot-id",
                1,
                0,
                enterable,
                LocalDateTime.of(2026, Month.JUNE, 16, 12, 5)
        );
    }
}
