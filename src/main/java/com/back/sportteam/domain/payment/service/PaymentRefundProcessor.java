package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.infra.payment.toss.TossPaymentsClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PaymentRefundProcessor {

    private static final String MISSING_PAYMENT_KEY_MESSAGE = "Missing TossPayments payment key.";
    private static final int MAX_RETRY_COUNT = 3;
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15)
    );

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
            recordRefundFailure(refundCommand.refundId(), e.getMessage(), processedAt);
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
                        refund.failPermanently(MISSING_PAYMENT_KEY_MESSAGE, processedAt);
                        return Optional.<RefundCommand>empty();
                    }

                    refund.markProcessing(processedAt);
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
                .filter(Refund::isProcessing)
                .ifPresent(refund -> {
                    refund.getPayment().refund(amount, processedAt);
                    refund.complete(processedAt);
                }));
    }

    private void recordRefundFailure(String refundId, String failureReason, LocalDateTime processedAt) {
        transactionTemplate.executeWithoutResult(status -> refundRepository.findByIdForUpdate(refundId)
                .filter(Refund::isProcessing)
                .ifPresent(refund -> refund.recordFailure(
                        normalizeFailureReason(failureReason),
                        processedAt,
                        MAX_RETRY_COUNT,
                        retryDelay(refund.getRetryCount() + 1)
                )));
    }

    private Duration retryDelay(int nextRetryCount) {
        int index = Math.max(nextRetryCount - 1, 0);
        if (index >= RETRY_DELAYS.size()) {
            return RETRY_DELAYS.getLast();
        }
        return RETRY_DELAYS.get(index);
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
