package com.back.sportteam.batch.settlement;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler.settlement")
public record SettlementSchedulerProperties(
        String cron,
        int batchSize,
        int chunkSize
) {
}
