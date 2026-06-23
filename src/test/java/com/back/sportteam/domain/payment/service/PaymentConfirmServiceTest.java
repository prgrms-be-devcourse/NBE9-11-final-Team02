package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.dto.request.PaymentConfirmRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentConfirmResponse;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import com.back.sportteam.infra.payment.toss.TossPaymentsPaymentResponse;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPostProcessor paymentPostProcessor;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    private PaymentConfirmService paymentConfirmService;

    @BeforeEach
    void setUp() {
        paymentConfirmService = new PaymentConfirmService(
                paymentRepository,
                paymentPostProcessor,
                tossPaymentsClient,
                transactionTemplate
        );

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void confirmMarksPaymentAsPaidAndRunsPostProcessorWhenTossConfirmSucceeds() {
        Payment payment = createPendingParticipationPayment();
        PaymentConfirmRequest request = createRequest(10_000);
        TossPaymentsPaymentResponse tossResponse =
                new TossPaymentsPaymentResponse("payment-key", "mid_12345", "DONE", 10_000);
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        when(tossPaymentsClient.confirm("payment-key", "mid_12345", 10_000)).thenReturn(tossResponse);

        PaymentConfirmResponse response = paymentConfirmService.confirm("user-id", request);

        assertThat(response.merchantUid()).isEqualTo("mid_12345");
        assertThat(response.paymentKey()).isEqualTo("payment-key");
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPgTransactionId()).isEqualTo("payment-key");
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentPostProcessor).processPaidPayment(payment);
    }

    @Test
    void confirmDoesNotCallTossWhenRequestedAmountDoesNotMatchPaymentAmount() {
        Payment payment = createPendingParticipationPayment();
        PaymentConfirmRequest request = createRequest(9_000);
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentConfirmService.confirm("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verify(tossPaymentsClient, never()).confirm(any(), any(), any());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void confirmRejectsPaymentOwnedByAnotherUser() {
        Payment payment = createPendingParticipationPayment();
        PaymentConfirmRequest request = createRequest(10_000);
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentConfirmService.confirm("other-user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_ACCESS_DENIED);

        verify(tossPaymentsClient, never()).confirm(any(), any(), any());
    }

    @Test
    void confirmMarksParticipationPaymentAsFailedWithoutCancellingParticipantWhenTossConfirmFails() {
        Payment payment = createPendingParticipationPayment();
        PaymentConfirmRequest request = createRequest(10_000);
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        doThrow(new BusinessException(PaymentErrorCode.PAYMENT_FAILED))
                .when(tossPaymentsClient)
                .confirm("payment-key", "mid_12345", 10_000);

        assertThatThrownBy(() -> paymentConfirmService.confirm("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_FAILED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPgTransactionId()).isEqualTo("payment-key");
        verify(paymentPostProcessor, never()).processFailedPayment(any(Payment.class), any());
    }

    @Test
    void confirmRunsFailurePostProcessorWhenFacilityPaymentConfirmFails() {
        Payment payment = Payment.create(
                null,
                "user-id",
                null,
                "facility-slot-id",
                PaymentType.FACILITY,
                "mid_12345",
                10_000
        );
        PaymentConfirmRequest request = createRequest(10_000);
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        doThrow(new BusinessException(PaymentErrorCode.PAYMENT_FAILED))
                .when(tossPaymentsClient)
                .confirm("payment-key", "mid_12345", 10_000);

        assertThatThrownBy(() -> paymentConfirmService.confirm("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_FAILED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentPostProcessor).processFailedPayment(any(Payment.class), any());
    }

    @Test
    void confirmKeepsPaymentPendingWhenTossConfirmResultIsUnknown() {
        Payment payment = createPendingParticipationPayment();
        PaymentConfirmRequest request = createRequest(10_000);
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        doThrow(new BusinessException(PaymentErrorCode.PAYMENT_CONFIRM_STATUS_UNKNOWN))
                .when(tossPaymentsClient)
                .confirm("payment-key", "mid_12345", 10_000);

        assertThatThrownBy(() -> paymentConfirmService.confirm("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_CONFIRM_STATUS_UNKNOWN);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPgTransactionId()).isNull();
        verify(paymentPostProcessor, never()).processFailedPayment(any(Payment.class), any());
    }

    @Test
    void confirmRejectsMismatchedTossConfirmResponse() {
        Payment payment = createPendingParticipationPayment();
        PaymentConfirmRequest request = createRequest(10_000);
        TossPaymentsPaymentResponse tossResponse =
                new TossPaymentsPaymentResponse("payment-key", "different-order-id", "DONE", 10_000);
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        when(tossPaymentsClient.confirm("payment-key", "mid_12345", 10_000)).thenReturn(tossResponse);

        assertThatThrownBy(() -> paymentConfirmService.confirm("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_PROVIDER_VERIFICATION_FAILED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentPostProcessor, never()).processPaidPayment(any());
    }

    private Payment createPendingParticipationPayment() {
        return Payment.create(
                "participant-id",
                "user-id",
                "match-id",
                null,
                PaymentType.PARTICIPATION,
                "mid_12345",
                10_000
        );
    }

    private PaymentConfirmRequest createRequest(Integer amount) {
        return new PaymentConfirmRequest("payment-key", "mid_12345", amount);
    }
}
