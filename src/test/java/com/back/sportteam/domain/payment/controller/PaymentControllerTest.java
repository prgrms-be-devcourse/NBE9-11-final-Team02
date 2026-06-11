package com.back.sportteam.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.service.PaymentService;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.PaymentErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void prepareReturnsPaymentOrder() throws Exception {
        PaymentPrepareResponse response = new PaymentPrepareResponse(
                "mid_12345",
                100_000L
        );
        when(paymentService.prepare(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchId": 50,
                                  "amount": 100000,
                                  "paymentType": "FACILITY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.merchantUid").value("mid_12345"))
                .andExpect(jsonPath("$.data.amount").value(100000));
    }

    @Test
    void prepareReturnsBadRequestWhenAmountIsTampered() throws Exception {
        when(paymentService.prepare(any()))
                .thenThrow(new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH));

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchId": 50,
                                  "amount": 1000,
                                  "paymentType": "PARTICIPATION"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PAYMENT_002"))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.path").value("/api/v1/payments/prepare"));
    }
}
