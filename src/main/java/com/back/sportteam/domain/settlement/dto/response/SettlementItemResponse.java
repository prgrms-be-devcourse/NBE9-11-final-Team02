package com.back.sportteam.domain.settlement.dto.response;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.settlement.entity.Settlement;
import com.back.sportteam.domain.settlement.entity.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementItemResponse(
        String id,
        String matchId,
        String hostId,
        SportType sportType,
        int totalParticipantFee,
        BigDecimal appliedFeeRate,
        int platformFee,
        int hostSettlementAmount,
        SettlementStatus status,
        LocalDateTime createdAt
) {
    public static SettlementItemResponse from(Settlement settlement) {
        return new SettlementItemResponse(
                settlement.getId(),
                settlement.getMatchId(),
                settlement.getHostId(),
                settlement.getSportType(),
                settlement.getTotalParticipantFee(),
                settlement.getAppliedFeeRate(),
                settlement.getPlatformFee(),
                settlement.getHostSettlementAmount(),
                settlement.getStatus(),
                settlement.getCreatedAt()
        );
    }
}
