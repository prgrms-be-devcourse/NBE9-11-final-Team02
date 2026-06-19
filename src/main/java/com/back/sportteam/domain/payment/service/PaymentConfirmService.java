package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.dto.request.PaymentConfirmRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentConfirmResponse;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.util.TimeUtils;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import com.back.sportteam.infra.payment.toss.TossPaymentsPaymentResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PaymentConfirmService {

    private static final String TOSS_DONE_STATUS = "DONE";

    private final PaymentRepository paymentRepository;
    private final PaymentPostProcessor paymentPostProcessor;
    private final TossPaymentsClient tossPaymentsClient;
    private final TransactionTemplate transactionTemplate;

    public PaymentConfirmResponse confirm(String userId, PaymentConfirmRequest request) {
        validateBeforeConfirm(userId, request);

        TossPaymentsPaymentResponse tossResponse;
        try {
            tossResponse = tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), request.amount());
        } catch (BusinessException e) {
            if (e.getErrorCode() == PaymentErrorCode.PAYMENT_FAILED) {
                recordConfirmFailure(userId, request);
            }
            throw e;
        }

        validateTossConfirmResult(request, tossResponse);
        return applyConfirmResult(userId, request, tossResponse);
    }

    private void validateBeforeConfirm(String userId, PaymentConfirmRequest request) {
        transactionTemplate.executeWithoutResult(status -> {
            Payment payment = getPaymentForUpdate(request.orderId());
            validateOwnership(payment, userId);
            validatePending(payment);
            validateAmount(payment, request.amount());
        });
    }

    private PaymentConfirmResponse applyConfirmResult(
            String userId,
            PaymentConfirmRequest request,
            TossPaymentsPaymentResponse tossResponse
    ) {
        return transactionTemplate.execute(status -> {
            Payment payment = getPaymentForUpdate(request.orderId());
            validateOwnership(payment, userId);
            validateAmount(payment, tossResponse.totalAmount());
            payment.complete(tossResponse.paymentKey(), LocalDateTime.now(TimeUtils.SERVICE_ZONE));
            paymentPostProcessor.processPaidPayment(payment);
            return PaymentConfirmResponse.from(payment);
        });
    }

    private void recordConfirmFailure(String userId, PaymentConfirmRequest request) {
        transactionTemplate.executeWithoutResult(status -> {
            Payment payment = getPaymentForUpdate(request.orderId());
            validateOwnership(payment, userId);
            validateAmount(payment, request.amount());
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.fail(request.paymentKey());
                paymentPostProcessor.processFailedPayment(payment, LocalDateTime.now(TimeUtils.SERVICE_ZONE));
            }
        });
    }

    private void validateTossConfirmResult(PaymentConfirmRequest request, TossPaymentsPaymentResponse response) {
        if (response == null
                || !request.paymentKey().equals(response.paymentKey())
                || !request.orderId().equals(response.orderId())
                || !request.amount().equals(response.totalAmount())
                || !TOSS_DONE_STATUS.equals(response.status())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_PROVIDER_VERIFICATION_FAILED);
        }
    }

    private Payment getPaymentForUpdate(String merchantUid) {
        return paymentRepository.findByMerchantUidForUpdate(merchantUid)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateOwnership(Payment payment, String userId) {
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }
    }

    private void validatePending(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    private void validateAmount(Payment payment, Integer amount) {
        if (!payment.getAmount().equals(amount)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }
}
