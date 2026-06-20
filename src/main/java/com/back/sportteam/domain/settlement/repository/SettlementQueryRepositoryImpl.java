package com.back.sportteam.domain.settlement.repository;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.settlement.dto.response.SettlementItemResponse;
import com.back.sportteam.domain.settlement.dto.response.SettlementSummaryResponse;
import com.back.sportteam.domain.settlement.entity.QSettlement;
import com.back.sportteam.domain.settlement.entity.Settlement;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SettlementQueryRepositoryImpl implements SettlementQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QSettlement s = QSettlement.settlement;

    @Override
    public SettlementSummaryResponse summarize(LocalDate from, LocalDate to) {
        var totalResult = queryFactory
                .select(
                        s.count(),
                        s.totalParticipantFee.sum().coalesce(0),
                        s.platformFee.sum().coalesce(0),
                        s.hostSettlementAmount.sum().coalesce(0)
                )
                .from(s)
                .where(createdAtBetween(from, to))
                .fetchOne();

        SettlementSummaryResponse.Total total = totalResult == null
                ? new SettlementSummaryResponse.Total(0, 0, 0, 0)
                : new SettlementSummaryResponse.Total(
                        totalResult.get(s.count()),
                        totalResult.get(s.totalParticipantFee.sum().coalesce(0)),
                        totalResult.get(s.platformFee.sum().coalesce(0)),
                        totalResult.get(s.hostSettlementAmount.sum().coalesce(0))
                );

        List<SettlementSummaryResponse.SportTypeBreakdown> breakdown = queryFactory
                .select(s.sportType, s.count(), s.platformFee.sum().coalesce(0))
                .from(s)
                .where(createdAtBetween(from, to))
                .groupBy(s.sportType)
                .orderBy(s.platformFee.sum().desc())
                .fetch()
                .stream()
                .map(this::toBreakdown)
                .toList();

        return new SettlementSummaryResponse(from, to, total, breakdown);
    }

    @Override
    public Page<SettlementItemResponse> findAll(LocalDate from, LocalDate to, SportType sportType, Pageable pageable) {
        List<Settlement> content = queryFactory
                .selectFrom(s)
                .where(
                        createdAtBetween(from, to),
                        sportTypeEq(sportType)
                )
                .orderBy(s.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(s.count())
                .from(s)
                .where(
                        createdAtBetween(from, to),
                        sportTypeEq(sportType)
                )
                .fetchOne();

        return new PageImpl<>(
                content.stream().map(SettlementItemResponse::from).toList(),
                pageable,
                total != null ? total : 0L
        );
    }

    private SettlementSummaryResponse.SportTypeBreakdown toBreakdown(com.querydsl.core.Tuple t) {
        return new SettlementSummaryResponse.SportTypeBreakdown(
                t.get(s.sportType),
                t.get(s.count()),
                t.get(s.platformFee.sum().coalesce(0))
        );
    }

    private BooleanExpression createdAtBetween(LocalDate from, LocalDate to) {
        return s.createdAt.between(
                LocalDateTime.of(from, LocalTime.MIN),
                LocalDateTime.of(to, LocalTime.MAX)
        );
    }

    private BooleanExpression sportTypeEq(SportType sportType) {
        return sportType != null ? s.sportType.eq(sportType) : null;
    }
}
