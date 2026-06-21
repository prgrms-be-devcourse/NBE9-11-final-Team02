package com.back.sportteam.domain.settlement.service;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.settlement.dto.response.SettlementItemResponse;
import com.back.sportteam.domain.settlement.dto.response.SettlementSummaryResponse;
import com.back.sportteam.domain.settlement.repository.SettlementQueryRepository;
import com.back.sportteam.global.util.TimeUtils;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementAdminService {

    private final SettlementQueryRepository settlementQueryRepository;

    @Transactional(readOnly = true)
    public SettlementSummaryResponse getSummary(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return settlementQueryRepository.summarize(from, to);
    }

    @Transactional(readOnly = true)
    public Page<SettlementItemResponse> getSettlements(LocalDate from, LocalDate to, SportType sportType, Pageable pageable) {
        validateDateRange(from, to);
        return settlementQueryRepository.findAll(from, to, sportType, pageable);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }
        if (to.isAfter(LocalDate.now(TimeUtils.SERVICE_ZONE))) {
            throw new IllegalArgumentException("조회 종료일은 오늘 이후일 수 없습니다.");
        }
    }
}
