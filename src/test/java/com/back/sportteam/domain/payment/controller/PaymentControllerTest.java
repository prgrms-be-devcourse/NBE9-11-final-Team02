package com.back.sportteam.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.service.PaymentService;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PaymentControllerTest {

    private PaymentService paymentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 결제_준비_요청은_검증된_주문_정보를_반환한다() throws Exception {
        PaymentPrepareResponse response = new PaymentPrepareResponse("mid_12345", 10_000);
        when(paymentService.prepare(any(String.class), any(PaymentPrepareRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .header("X-USER-ID", "user-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.merchantUid").value("mid_12345"))
                .andExpect(jsonPath("$.data.amount").value(10000));

        verify(paymentService).prepare(any(String.class), any(PaymentPrepareRequest.class));
    }

    @Test
    void 매칭_ID가_비어_있으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/payments/prepare")
                        .header("X-USER-ID", "user-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matchId": "",
                                  "amount": 10000,
                                  "paymentType": "PARTICIPATION"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_002"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/payments/prepare"));
    }

    @Test
    void 결제_금액이_일치하지_않으면_400_응답을_반환한다() throws Exception {
        when(paymentService.prepare(any(String.class), any(PaymentPrepareRequest.class)))
                .thenThrow(new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH));

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .header("X-USER-ID", "user-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PAYMENT_002"))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.path").value("/api/v1/payments/prepare"));
    }

    @Test
    void 사용자_ID_헤더가_없으면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_002"));
    }

    private String validRequestJson() {
        return """
                {
                  "matchId": "match-id",
                  "amount": 10000,
                  "paymentType": "PARTICIPATION"
                }
                """;
    }
}
