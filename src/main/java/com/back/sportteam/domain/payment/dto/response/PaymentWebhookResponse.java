package com.back.sportteam.domain.payment.dto.response;

public record PaymentWebhookResponse(String status) {

    private static final String OK = "OK";

    public static PaymentWebhookResponse ok() {
        return new PaymentWebhookResponse(OK);
    }
}
