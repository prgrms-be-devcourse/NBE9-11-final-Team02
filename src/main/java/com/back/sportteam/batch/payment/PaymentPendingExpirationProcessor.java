package com.back.sportteam.batch.payment;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.service.PaymentPostProcessor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentPendingExpirationProcessor {

    private final PaymentRepository paymentRepository;
    private final PaymentPostProcessor paymentPostProcessor;

    @Transactional
    public int expire(LocalDateTime threshold, LocalDateTime processedAt, int batchSize) {
        List<Payment> payments = paymentRepository.findStalePendingPaymentsForUpdate(
                PaymentStatus.PENDING,
                threshold,
                PageRequest.of(0, batchSize)
        );

        payments.forEach(payment -> {
            payment.fail(null);
            paymentPostProcessor.processFailedPayment(payment, processedAt);
        });

        return payments.size();
    }
}
