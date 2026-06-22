package com.back.sportteam.batch.settlement;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.settlement.entity.Settlement;
import com.back.sportteam.domain.settlement.policy.SettlementPolicy;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementProcessor {

    private final MatchRepository matchRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementPolicy settlementPolicy;

    @Transactional
    public void process(String matchId) {
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != MatchStatus.COMPLETED) {
            return;
        }

        Long totalParticipantFee = paymentRepository.sumAmountByMatchId(
                matchId,
                PaymentType.PARTICIPATION,
                PaymentStatus.PAID
        );
        if (totalParticipantFee == null) {
            throw new IllegalStateException("COMPLETED 경기에 PAID 참가비가 없습니다. matchId=" + matchId);
        }

        Settlement settlement = Settlement.create(
                matchId,
                match.getHostId(),
                match.getSportType(),
                Math.toIntExact(totalParticipantFee.longValue()),
                settlementPolicy.getPlatformFeeRate()
        );
        settlementRepository.save(settlement);
    }
}
