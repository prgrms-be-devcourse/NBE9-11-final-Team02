package com.back.sportteam.domain.match.repository;

import static com.back.sportteam.domain.match.entity.QMatch.match;

import com.back.sportteam.domain.match.dto.request.MatchSearchCondition;
import com.back.sportteam.domain.match.dto.request.MatchSortType;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MatchQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Match> findAll(MatchSearchCondition condition) {
        Pageable pageable = condition.toPageable();
        List<Match> content = queryFactory
                .selectFrom(match)
                .where(
                        sportTypeEq(condition.sportType()),
                        statusEq(condition.status()),
                        minSkillLevelEq(condition.minSkillLevel()),
                        maxSkillLevelEq(condition.maxSkillLevel()),
                        requiredGenderEq(condition.requiredGender())
                )
                .orderBy(orderSpecifiers(condition.sort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(match.count())
                .from(match)
                .where(
                        sportTypeEq(condition.sportType()),
                        statusEq(condition.status()),
                        minSkillLevelEq(condition.minSkillLevel()),
                        maxSkillLevelEq(condition.maxSkillLevel()),
                        requiredGenderEq(condition.requiredGender())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    public List<Match> findRecommendationCandidates(
            SportType sportType,
            MatchStatus status,
            LocalDateTime now,
            int limit
    ) {
        return queryFactory
                .selectFrom(match)
                .where(
                        sportTypeEq(sportType),
                        statusEq(status),
                        notFull(),
                        recruitDeadlineAfter(now)
                )
                .orderBy(match.recruitDeadline.asc(), match.id.asc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression sportTypeEq(SportType sportType) {
        return sportType == null ? null : match.sportType.eq(sportType);
    }

    private BooleanExpression statusEq(MatchStatus status) {
        return status == null ? null : match.status.eq(status);
    }

    private BooleanExpression minSkillLevelEq(SkillLevel minSkillLevel) {
        return minSkillLevel == null ? null : match.minSkillLevel.eq(minSkillLevel);
    }

    private BooleanExpression maxSkillLevelEq(SkillLevel maxSkillLevel) {
        return maxSkillLevel == null ? null : match.maxSkillLevel.eq(maxSkillLevel);
    }

    private BooleanExpression requiredGenderEq(RequiredGender requiredGender) {
        return requiredGender == null ? null : match.requiredGender.eq(requiredGender);
    }

    private BooleanExpression notFull() {
        return match.currentCount.lt(match.capacity);
    }

    private BooleanExpression recruitDeadlineAfter(LocalDateTime now) {
        return match.recruitDeadline.after(now);
    }

    private OrderSpecifier<?>[] orderSpecifiers(MatchSortType sortType) {
        MatchSortType resolvedSort = sortType == null ? MatchSortType.LATEST : sortType;
        return switch (resolvedSort) {
            case LATEST -> new OrderSpecifier<?>[]{match.createdAt.desc(), match.id.desc()};
            case DEADLINE_ASC -> new OrderSpecifier<?>[]{match.recruitDeadline.asc(), match.id.asc()};
            case FEE_ASC -> new OrderSpecifier<?>[]{match.feePerPerson.asc(), match.id.asc()};
            case PARTICIPANT_DESC -> new OrderSpecifier<?>[]{match.currentCount.desc(), match.id.desc()};
        };
    }
}
