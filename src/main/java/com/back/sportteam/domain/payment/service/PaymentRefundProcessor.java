package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import java.time.LocalDateTime;
import java.util.Optional;
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
        Optional<RefundCommand> command = claimRefund(refundId, processedAt);
        if (command.isEmpty()) {
            return;
        }

        RefundCommand refundCommand = command.get();
        try {
            tossPaymentsClient.cancelPayment(
                    refundCommand.paymentKey(),
                    refundCommand.amount(),
                    refundCommand.reason()
            );
        } catch (RuntimeException e) {
            failRefund(refundCommand.refundId(), e.getMessage());
            return;
        }

        completeRefund(refundCommand.refundId(), refundCommand.amount(), processedAt);
    }

    private Optional<RefundCommand> claimRefund(String refundId, LocalDateTime processedAt) {
        return transactionTemplate.execute(status -> refundRepository.findByIdForUpdate(refundId)
                .filter(Refund::isPending)
                .map(refund -> {
                    Payment payment = refund.getPayment();
                    if (payment.getPgTransactionId() == null || payment.getPgTransactionId().isBlank()) {
                        refund.fail(MISSING_PAYMENT_KEY_MESSAGE);
                        return Optional.<RefundCommand>empty();
                    }

                    return Optional.of(new RefundCommand(
                            refund.getId(),
                            payment.getPgTransactionId(),
                            refund.getAmount(),
                            refund.getReason()
                    ));
                })
                .orElseGet(Optional::empty));
    }

    private void completeRefund(String refundId, Integer amount, LocalDateTime processedAt) {
        transactionTemplate.executeWithoutResult(status -> refundRepository.findByIdForUpdate(refundId)
                .filter(Refund::isPending)
                .ifPresent(refund -> {
                    refund.getPayment().refund(amount, processedAt);
                    refund.complete(processedAt);
                }));
    }

    private void failRefund(String refundId, String failureReason) {
        transactionTemplate.executeWithoutResult(status -> refundRepository.findByIdForUpdate(refundId)
                .filter(Refund::isPending)
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
