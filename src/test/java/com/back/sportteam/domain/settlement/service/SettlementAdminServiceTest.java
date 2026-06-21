package com.back.sportteam.domain.settlement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.back.sportteam.domain.settlement.repository.SettlementQueryRepository;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SettlementAdminServiceTest {

    @Mock
    private SettlementQueryRepository settlementQueryRepository;

    @InjectMocks
    private SettlementAdminService settlementAdminService;

    @Test
    void 시작일이_종료일보다_늦으면_예외가_발생한다() {
        LocalDate from = LocalDate.of(2026, Month.JUNE, 30);
        LocalDate to = LocalDate.of(2026, Month.JUNE, 1);

        assertThatThrownBy(() -> settlementAdminService.getSummary(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시작일이 종료일보다 늦을 수 없습니다");

        verify(settlementQueryRepository, never()).summarize(any(), any());
    }

    @Test
    void 종료일이_오늘_이후면_예외가_발생한다() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> settlementAdminService.getSummary(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("조회 종료일은 오늘 이후일 수 없습니다");

        verify(settlementQueryRepository, never()).summarize(any(), any());
    }

    @Test
    void 목록_조회에서_시작일이_종료일보다_늦으면_예외가_발생한다() {
        LocalDate from = LocalDate.of(2026, Month.JUNE, 30);
        LocalDate to = LocalDate.of(2026, Month.JUNE, 1);
        Pageable pageable = Pageable.unpaged();

        assertThatThrownBy(() -> settlementAdminService.getSettlements(from, to, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시작일이 종료일보다 늦을 수 없습니다");

        verify(settlementQueryRepository, never()).findAll(any(), any(), any(), any());
    }
}
