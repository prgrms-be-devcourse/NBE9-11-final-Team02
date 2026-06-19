package com.back.sportteam.infra.payment.toss;

public interface TossPaymentsClient {

    TossPaymentsPaymentResponse confirm(String paymentKey, String orderId, Integer amount);

    TossPaymentsPaymentResponse getPayment(String paymentKey);

    void cancelPayment(String paymentKey, Integer cancelAmount, String cancelReason);
}
