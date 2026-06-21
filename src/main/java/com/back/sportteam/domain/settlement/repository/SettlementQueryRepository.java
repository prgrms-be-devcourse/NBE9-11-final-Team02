package com.back.sportteam.domain.settlement.repository;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.settlement.dto.response.SettlementItemResponse;
import com.back.sportteam.domain.settlement.dto.response.SettlementSummaryResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettlementQueryRepository {

    SettlementSummaryResponse summarize(LocalDate from, LocalDate to);

    Page<SettlementItemResponse> findAll(LocalDate from, LocalDate to, SportType sportType, Pageable pageable);
}
