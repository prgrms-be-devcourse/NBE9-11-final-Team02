package com.back.sportteam.domain.settlement.dto.response;

import com.back.sportteam.domain.match.entity.SportType;
import java.time.LocalDate;
import java.util.List;

public record SettlementSummaryResponse(
        LocalDate from,
        LocalDate to,
        Total total,
        List<SportTypeBreakdown> breakdown
) {
    public record Total(
            long count,
            long totalParticipantFee,
            long totalPlatformFee,
            long totalHostSettlementAmount
    ) {}

    public record SportTypeBreakdown(
            SportType sportType,
            long count,
            long totalPlatformFee
    ) {}
}
