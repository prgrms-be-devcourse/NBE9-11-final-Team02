package com.back.sportteam.domain.settlement.policy;

import com.back.sportteam.domain.settlement.config.SettlementProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ConfigBasedSettlementPolicy implements SettlementPolicy {

    private final SettlementProperties settlementProperties;

    @Override
    public BigDecimal getPlatformFeeRate() {
        return settlementProperties.platformFeeRate();
    }
}
