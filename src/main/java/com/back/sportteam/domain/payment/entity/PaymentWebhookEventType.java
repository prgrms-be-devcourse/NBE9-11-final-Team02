package com.back.sportteam.domain.payment.entity;

public enum PaymentWebhookEventType {
    PAYMENT_SUCCEEDED(PaymentStatus.PAID),
    PAYMENT_FAILED(PaymentStatus.FAILED);

    private final PaymentStatus paymentStatus;

    PaymentWebhookEventType(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
}
