package com.back.sportteam.domain.facility.repository;

import static com.back.sportteam.domain.facility.entity.QFacility.facility;
import static com.back.sportteam.domain.facility.entity.QFacilitySlot.facilitySlot;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.match.entity.SportType;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class FacilityQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Facility> findAvailable(SportType sportType, String region, LocalDate date, Pageable pageable) {
        var query = queryFactory
                .selectFrom(facility)
                .where(
                        facility.status.eq(FacilityStatus.ACTIVE),
                        sportTypeCondition(sportType),
                        regionCondition(region),
                        dateCondition(date)
                );

        long total = query.fetch().size();

        List<Facility> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private com.querydsl.core.types.dsl.BooleanExpression sportTypeCondition(SportType sportType) {
        if (sportType == null) return null;
        return facility.sportTypes.contains(sportType);
    }

    private com.querydsl.core.types.dsl.BooleanExpression regionCondition(String region) {
        if (!StringUtils.hasText(region)) return null;
        return facility.address.containsIgnoreCase(region);
    }

    private com.querydsl.core.types.dsl.BooleanExpression dateCondition(LocalDate date) {
        if (date == null) return null;
        return JPAExpressions
                .selectOne()
                .from(facilitySlot)
                .where(
                        facilitySlot.facilityId.eq(facility.id),
                        facilitySlot.slotDate.eq(date),
                        facilitySlot.status.eq(SlotStatus.AVAILABLE)
                )
                .exists();
    }
}