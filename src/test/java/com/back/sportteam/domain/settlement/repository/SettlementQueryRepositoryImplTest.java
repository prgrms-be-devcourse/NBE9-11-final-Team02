package com.back.sportteam.domain.settlement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.settlement.dto.response.SettlementItemResponse;
import com.back.sportteam.domain.settlement.dto.response.SettlementSummaryResponse;
import com.back.sportteam.domain.settlement.entity.Settlement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettlementQueryRepositoryImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, Month.JUNE, 1);
    private static final LocalDate TO = LocalDate.of(2026, Month.JUNE, 30);
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0700");

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private SettlementQueryRepository settlementQueryRepository;

    @Test
    void 기간_내_전체_집계가_올바르게_계산된다() {
        saveSettlement("match-1", SportType.FUTSAL, 30_000);
        saveSettlement("match-2", SportType.FUTSAL, 50_000);
        saveSettlement("match-3", SportType.BASKETBALL, 40_000);

        SettlementSummaryResponse result = settlementQueryRepository.summarize(FROM, TO);

        assertThat(result.total().count()).isEqualTo(3);
        assertThat(result.total().totalParticipantFee()).isEqualTo(120_000);
        assertThat(result.total().totalPlatformFee()).isEqualTo(8_400);
        assertThat(result.total().totalHostSettlementAmount()).isEqualTo(111_600);
    }

    @Test
    void 종목별_수수료_집계가_플랫폼_수수료_합계_높은_순으로_반환된다() {
        saveSettlement("match-1", SportType.FUTSAL, 30_000);
        saveSettlement("match-2", SportType.FUTSAL, 50_000);
        saveSettlement("match-3", SportType.BASKETBALL, 40_000);

        SettlementSummaryResponse result = settlementQueryRepository.summarize(FROM, TO);

        assertThat(result.breakdown()).hasSize(2);
        assertThat(result.breakdown().get(0).sportType()).isEqualTo(SportType.FUTSAL);
        assertThat(result.breakdown().get(0).count()).isEqualTo(2);
        assertThat(result.breakdown().get(1).sportType()).isEqualTo(SportType.BASKETBALL);
    }

    @Test
    void 조회_기간_외에_생성된_정산은_집계_결과에_포함되지_않는다() {
        saveSettlement("match-1", SportType.FUTSAL, 30_000);

        SettlementSummaryResponse result = settlementQueryRepository.summarize(
                LocalDate.of(2026, Month.JULY, 1),
                LocalDate.of(2026, Month.JULY, 31)
        );

        assertThat(result.total().count()).isZero();
        assertThat(result.breakdown()).isEmpty();
    }

    @Test
    void 목록_조회시_종목_필터가_적용된다() {
        saveSettlement("match-1", SportType.FUTSAL, 30_000);
        saveSettlement("match-2", SportType.BASKETBALL, 40_000);

        Page<SettlementItemResponse> result = settlementQueryRepository.findAll(
                FROM, TO, SportType.FUTSAL, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).sportType()).isEqualTo(SportType.FUTSAL);
    }

    @Test
    void 종목_필터를_지정하지_않으면_모든_종목의_정산이_반환된다() {
        saveSettlement("match-1", SportType.FUTSAL, 30_000);
        saveSettlement("match-2", SportType.BASKETBALL, 40_000);

        Page<SettlementItemResponse> result = settlementQueryRepository.findAll(
                FROM, TO, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    private Settlement saveSettlement(String matchId, SportType sportType, int totalParticipantFee) {
        return settlementRepository.save(
                Settlement.create(matchId, "host-id", sportType, totalParticipantFee, FEE_RATE)
        );
    }
}
