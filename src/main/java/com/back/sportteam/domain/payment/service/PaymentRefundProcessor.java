package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRefundProcessor {

    private static final String MISSING_PAYMENT_KEY = "결제 키가 없어 환불을 요청할 수 없습니다.";

    private final RefundRepository refundRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public void process(String refundId, LocalDateTime processedAt) {
        Refund refund = refundRepository.findByIdForUpdate(refundId).orElse(null);
        if (refund == null || !refund.isPending()) {
            return;
        }

        Payment payment = refund.getPayment();
        if (payment.getPgTransactionId() == null || payment.getPgTransactionId().isBlank()) {
            refund.fail(MISSING_PAYMENT_KEY);
            return;
        }

        try {
            tossPaymentsClient.cancelPayment(
                    payment.getPgTransactionId(),
                    refund.getAmount(),
                    refund.getReason()
            );
            payment.refund(refund.getAmount(), processedAt);
            refund.complete(processedAt);
        } catch (BusinessException e) {
            if (e.getErrorCode() != PaymentErrorCode.REFUND_FAILED) {
                throw e;
            }
            refund.fail(e.getMessage());
        }
        return failureReason;
    }

    private record RefundCommand(
            String refundId,
            String paymentKey,
            Integer amount,
            String reason
    ) {
    }
}
