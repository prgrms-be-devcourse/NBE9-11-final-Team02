package com.back.sportteam.domain.payment.controller;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.request.PaymentConfirmRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.dto.response.PaymentConfirmResponse;
import com.back.sportteam.domain.payment.service.PaymentConfirmService;
import com.back.sportteam.domain.payment.service.PaymentService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentConfirmService paymentConfirmService;

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepare(
            @AuthenticationPrincipal @NotBlank(message = "사용자 ID는 필수입니다.") String userId,
            @RequestParam(required = false) String queueToken,
            @Valid @RequestBody PaymentPrepareRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.prepare(userId, queueToken, request)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirm(
            @AuthenticationPrincipal @NotBlank(message = "사용자 ID는 필수입니다.") String userId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(paymentConfirmService.confirm(userId, request)));
    }
}
