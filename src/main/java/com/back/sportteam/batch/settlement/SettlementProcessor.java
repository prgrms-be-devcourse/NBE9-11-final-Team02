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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementProcessor {

    private final MatchRepository matchRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementPolicy settlementPolicy;

    @Transactional
    public void processBatch(List<String> matchIds) {
        List<Match> matches = matchRepository.findAllById(matchIds);
        Map<String, Long> feeMap = paymentRepository.sumAmountMapByMatchIds(
                matchIds, PaymentType.PARTICIPATION, PaymentStatus.PAID);

        List<Settlement> results = new ArrayList<>();
        for (Match match : matches) {
            if (match.getStatus() != MatchStatus.COMPLETED) {
                log.error("[Settlement] 데이터 정합성 오류 - 완료 상태가 아닌 경기가 배치에 포함됨. matchId={}, status={}", match.getId(), match.getStatus());
            } else {
                long fee = feeMap.getOrDefault(match.getId(), 0L);
                if (match.getFeePerPerson() > 0 && fee == 0) {
                    log.error("[Settlement] 유료 경기인데 PAID 없음 - 운영자 확인 필요. matchId={}", match.getId());
                } else {
                    results.add(Settlement.create(
                            match.getId(), match.getHostId(), match.getSportType(),
                            Math.toIntExact(fee), settlementPolicy.getPlatformFeeRate()));
                }
            }
        }
        settlementRepository.saveAll(results);
    }
}
