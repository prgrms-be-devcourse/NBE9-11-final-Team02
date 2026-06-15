package com.back.sportteam.domain.payment.controller;

import com.back.sportteam.domain.payment.dto.response.PaymentWebhookResponse;
import com.back.sportteam.domain.payment.service.TossPaymentsWebhookService;
import com.back.sportteam.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/webhook")
public class TossPaymentsWebhookController {

    private final TossPaymentsWebhookService tossPaymentsWebhookService;

    @PostMapping({"", "/tosspayments"})
    public ResponseEntity<ApiResponse<PaymentWebhookResponse>> webhook(
            @RequestHeader("tosspayments-webhook-transmission-id") String transmissionId,
            @RequestBody String payload
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                tossPaymentsWebhookService.handle(transmissionId, payload)
        ));
    }
}
