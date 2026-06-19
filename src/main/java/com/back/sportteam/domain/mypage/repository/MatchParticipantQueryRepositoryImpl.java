package com.back.sportteam.domain.mypage.repository;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.QMatch;
import com.back.sportteam.domain.match.entity.QMatchParticipant;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.MyMatchStatus;
import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MatchParticipantQueryRepositoryImpl implements MatchParticipantQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QMatchParticipant mp = QMatchParticipant.matchParticipant;
    private static final QMatch m = QMatch.match;

    @Override
    public Page<MyMatchResponse> findMyMatches(String userId, MyMatchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(mp.userId.eq(userId));
        builder.and(toSportTypeCondition(condition.sportType()));
        builder.and(toRoleCondition(condition.role()));
        builder.and(toMyMatchStatusCondition(condition.myMatchStatus()));

        Pageable pageable = condition.toPageable();

        List<Tuple> rawContent = queryFactory
                .select(m.id, m.title, m.sportType, m.status, mp.status, mp.role, m.matchDate, m.startTime, m.endTime)
                .from(mp)
                .join(mp.match, m)
                .where(builder)
                .orderBy(m.matchDate.desc(), m.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<MyMatchResponse> content = rawContent.stream()
                .map(tuple -> MyMatchResponse.of(
                        tuple.get(m.id),
                        tuple.get(m.title),
                        tuple.get(m.sportType),
                        tuple.get(m.status),
                        tuple.get(mp.status),
                        tuple.get(mp.role),
                        tuple.get(m.matchDate),
                        tuple.get(m.startTime),
                        tuple.get(m.endTime)
                ))
                .toList();

        Long total = queryFactory
                .select(mp.count())
                .from(mp)
                .join(mp.match, m)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression toSportTypeCondition(SportType sportType) {
        return sportType != null ? m.sportType.eq(sportType) : null;
    }

    private BooleanExpression toRoleCondition(MatchParticipantRole role) {
        return role != null ? mp.role.eq(role) : null;
    }

    private BooleanExpression toMyMatchStatusCondition(MyMatchStatus myMatchStatus) {
        if (myMatchStatus == null) return null;
        return switch (myMatchStatus) {
            case PARTICIPATING -> mp.status.eq(MatchParticipantStatus.ACTIVE)
                    .and(m.status.in(MatchStatus.RECRUITING, MatchStatus.CONFIRMED));
            case COMPLETED -> mp.status.eq(MatchParticipantStatus.ACTIVE)
                    .and(m.status.eq(MatchStatus.COMPLETED));
            case CANCELLED -> mp.status.eq(MatchParticipantStatus.ACTIVE)
                    .and(m.status.in(MatchStatus.RECRUITING, MatchStatus.CONFIRMED, MatchStatus.COMPLETED))
                    .not();
        };
    }
}
