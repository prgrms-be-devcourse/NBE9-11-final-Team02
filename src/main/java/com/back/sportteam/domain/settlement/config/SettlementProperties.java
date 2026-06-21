package com.back.sportteam.domain.settlement.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "app.settlement")
public record SettlementProperties(

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        BigDecimal platformFeeRate
) {
}
