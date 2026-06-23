package com.back.sportteam.infra.payment.toss;

import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TossPaymentsRestClient implements TossPaymentsClient {

    private final RestClient restClient;
    private final String authorization;

    public TossPaymentsRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.payment.toss.base-url:https://api.tosspayments.com}") String baseUrl,
            @Value("${app.payment.toss.secret-key:}") String secretKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.authorization = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public TossPaymentsPaymentResponse confirm(String paymentKey, String orderId, Integer amount) {
        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .body(new TossPaymentsConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossPaymentsPaymentResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError()) {
                throw new BusinessException(PaymentErrorCode.PAYMENT_CONFIRM_STATUS_UNKNOWN);
            }
            throw new BusinessException(PaymentErrorCode.PAYMENT_FAILED);
        } catch (ResourceAccessException _) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_CONFIRM_STATUS_UNKNOWN);
        } catch (RestClientException _) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_CONFIRM_STATUS_UNKNOWN);
        }
    }

    @Override
    public TossPaymentsPaymentResponse getPayment(String paymentKey) {
        try {
            return restClient.get()
                    .uri("/v1/payments/{paymentKey}", paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(TossPaymentsPaymentResponse.class);
        } catch (RestClientResponseException _) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_PROVIDER_VERIFICATION_FAILED);
        }
    }

    @Override
    public void cancelPayment(String paymentKey, Integer cancelAmount, String cancelReason) {
        try {
            restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .body(new TossPaymentsCancelRequest(cancelReason, cancelAmount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException _) {
            throw new BusinessException(PaymentErrorCode.REFUND_FAILED);
        }
    }
}
