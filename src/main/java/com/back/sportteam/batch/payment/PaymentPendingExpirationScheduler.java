package com.back.sportteam.batch.payment;

import com.back.sportteam.global.util.TimeUtils;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentPendingExpirationScheduler {

    private final PaymentPendingExpirationProcessor paymentPendingExpirationProcessor;

    @Value("${app.scheduler.payment-pending.expire-minutes:30}")
    private long expireMinutes;

    @Value("${app.scheduler.payment-pending.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.scheduler.payment-pending.fixed-delay-ms:600000}")
    public void expireStalePendingPayments() {
        LocalDateTime processedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
        LocalDateTime threshold = processedAt.minusMinutes(expireMinutes);

        paymentPendingExpirationProcessor.expire(threshold, processedAt, batchSize);
    }
}
