package com.back.sportteam.domain.payment.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.domain.payment.dto.response.PaymentWebhookResponse;
import com.back.sportteam.domain.payment.service.TossPaymentsWebhookService;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TossPaymentsWebhookControllerTest {

    private TossPaymentsWebhookService tossPaymentsWebhookService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tossPaymentsWebhookService = org.mockito.Mockito.mock(TossPaymentsWebhookService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TossPaymentsWebhookController(tossPaymentsWebhookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void tossPaymentsWebhookIsHandledWithTransmissionIdHeader() throws Exception {
        when(tossPaymentsWebhookService.handle("transmission-id", payload()))
                .thenReturn(PaymentWebhookResponse.ok());

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("tosspayments-webhook-transmission-id", "transmission-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OK"));

        verify(tossPaymentsWebhookService).handle("transmission-id", payload());
    }

    @Test
    void tossPaymentsWebhookAliasPathIsHandled() throws Exception {
        when(tossPaymentsWebhookService.handle("transmission-id", payload()))
                .thenReturn(PaymentWebhookResponse.ok());

        mockMvc.perform(post("/api/v1/payments/webhook/tosspayments")
                        .header("tosspayments-webhook-transmission-id", "transmission-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tossPaymentsWebhookService).handle("transmission-id", payload());
    }

    private String payload() {
        return """
                {
                  "eventType": "PAYMENT_STATUS_CHANGED",
                  "data": {
                    "paymentKey": "payment-key",
                    "orderId": "mid_12345",
                    "status": "DONE",
                    "totalAmount": 10000
                  }
                }
                """;
    }
}
