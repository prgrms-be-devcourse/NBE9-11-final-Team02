package com.back.sportteam.domain.payment.controller;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.service.PaymentService;
import com.back.sportteam.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepare(
            @RequestBody PaymentPrepareRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.prepare(request)));
    }
}
