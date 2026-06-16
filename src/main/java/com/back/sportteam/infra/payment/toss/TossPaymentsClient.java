package com.back.sportteam.infra.payment.toss;

public interface TossPaymentsClient {

    TossPaymentsPaymentResponse getPayment(String paymentKey);

    void cancelPayment(String paymentKey, Integer cancelAmount, String cancelReason);
}
