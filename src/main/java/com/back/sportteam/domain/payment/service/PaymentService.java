package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.PaymentErrorCode;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final String MERCHANT_UID_PREFIX = "mid_";

    private final PaymentRepository paymentRepository;
    private final ObjectProvider<PaymentAmountReader> paymentAmountReaderProvider;

    public PaymentService(
            PaymentRepository paymentRepository,
            ObjectProvider<PaymentAmountReader> paymentAmountReaderProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAmountReaderProvider = paymentAmountReaderProvider;
    }

    @Transactional
    public PaymentPrepareResponse prepare(PaymentPrepareRequest request) {
        validateRequest(request);

        Long expectedAmount = getExpectedAmount(request);
        if (!expectedAmount.equals(request.amount())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        Payment payment = Payment.create(
                generateMerchantUid(),
                request.matchId(),
                expectedAmount,
                request.paymentType()
        );

        return PaymentPrepareResponse.from(paymentRepository.save(payment));
    }

    private void validateRequest(PaymentPrepareRequest request) {
        if (request == null
                || request.amount() == null
                || request.amount() <= 0
                || request.paymentType() == null) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_REQUEST);
        }

        if (request.matchId() == null) {
            throw new BusinessException(PaymentErrorCode.INVALID_PAYMENT_REQUEST);
        }
    }

    private Long getExpectedAmount(PaymentPrepareRequest request) {
        PaymentAmountReader amountReader = paymentAmountReaderProvider.getIfAvailable();
        if (amountReader == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_SOURCE_UNAVAILABLE);
        }

        Long expectedAmount = switch (request.paymentType()) {
            case FACILITY -> amountReader.getFacilityAmount(request.matchId());
            case PARTICIPATION -> amountReader.getParticipationAmount(request.matchId());
        };

        if (expectedAmount == null || expectedAmount <= 0) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_SOURCE_UNAVAILABLE);
        }

        return expectedAmount;
    }

    private String generateMerchantUid() {
        return MERCHANT_UID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
