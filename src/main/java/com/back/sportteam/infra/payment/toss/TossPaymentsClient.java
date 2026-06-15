package com.back.sportteam.infra.payment.toss;

public interface TossPaymentsClient {

    TossPaymentsPaymentResponse getPayment(String paymentKey);
}
