package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PaymentRefundProcessor {

    private static final String MISSING_PAYMENT_KEY_MESSAGE = "Missing TossPayments payment key.";

    private final RefundRepository refundRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final TransactionTemplate transactionTemplate;

    public void process(String refundId, LocalDateTime processedAt) {
        RefundCommand command = claimRefund(refundId);
        if (command == null) {
            return;
        }

        try {
            tossPaymentsClient.cancelPayment(
                    command.paymentKey(),
                    command.amount(),
                    command.reason()
            );
        } catch (RuntimeException e) {
            failRefund(command.refundId(), e.getMessage());
            return;
        }

        completeRefund(command.refundId(), command.amount(), processedAt);
    }

    private RefundCommand claimRefund(String refundId) {
        return transactionTemplate.execute(status -> refundRepository.findByIdForUpdate(refundId)
                .filter(Refund::isPending)
                .map(refund -> {
                    Payment payment = refund.getPayment();
                    if (payment.getPgTransactionId() == null || payment.getPgTransactionId().isBlank()) {
                        refund.fail(MISSING_PAYMENT_KEY_MESSAGE);
                        return null;
                    }

                    refund.markProcessing();
                    return new RefundCommand(
                            refund.getId(),
                            payment.getPgTransactionId(),
                            refund.getAmount(),
                            refund.getReason()
                    );
                })
                .orElse(null));
    }

    private void completeRefund(String refundId, Integer amount, LocalDateTime processedAt) {
        transactionTemplate.executeWithoutResult(status -> refundRepository.findByIdForUpdate(refundId)
                .filter(Refund::isProcessing)
                .ifPresent(refund -> {
                    refund.getPayment().refund(amount, processedAt);
                    refund.complete(processedAt);
                }));
    }

    private void failRefund(String refundId, String failureReason) {
        transactionTemplate.executeWithoutResult(status -> refundRepository.findByIdForUpdate(refundId)
                .filter(Refund::isProcessing)
                .ifPresent(refund -> refund.fail(normalizeFailureReason(failureReason))));
    }

    private String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "TossPayments refund request failed.";
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
