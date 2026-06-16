package com.back.sportteam.batch.refund;

import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.domain.payment.service.PaymentRefundProcessor;
import com.back.sportteam.global.util.TimeUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundScheduler {

    private final RefundRepository refundRepository;
    private final PaymentRefundProcessor paymentRefundProcessor;

    @Value("${app.scheduler.refund.batch-size:50}")
    private int batchSize;

    @Value("${app.scheduler.refund.processing-timeout-minutes:10}")
    private long processingTimeoutMinutes;

    @Scheduled(fixedDelayString = "${app.scheduler.refund.fixed-delay-ms:60000}")
    public void processPendingRefunds() {
        LocalDateTime processedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
        List<String> refundIds = refundRepository.findIdsByStatus(
                RefundStatus.PENDING,
                processedAt.minusMinutes(processingTimeoutMinutes),
                processedAt
        );

        List<String> refundIds = refundRepository.findProcessableIds(
                RefundStatus.PENDING,
                processedAt,
                PageRequest.of(0, batchSize)
        );

        for (String refundId : refundIds) {
            processRefund(refundId, processedAt);
        }
    }

    private void processRefund(String refundId, LocalDateTime processedAt) {
        try {
            paymentRefundProcessor.process(refundId, processedAt);
        } catch (RuntimeException e) {
            log.error("Failed to process pending refund. refundId={}", refundId, e);
        }
    }
}
