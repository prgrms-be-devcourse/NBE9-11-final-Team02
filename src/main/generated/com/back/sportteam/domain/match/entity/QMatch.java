package com.back.sportteam.domain.match.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMatch is a Querydsl query type for Match
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatch extends EntityPathBase<Match> {

    private static final long serialVersionUID = -1979448522L;

    public static final QMatch match = new QMatch("match");

    public final DateTimePath<java.time.LocalDateTime> cancelDeadline = createDateTime("cancelDeadline", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> cancelledAt = createDateTime("cancelledAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> capacity = createNumber("capacity", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> confirmedAt = createDateTime("confirmedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> currentCount = createNumber("currentCount", Integer.class);

    public final TimePath<java.time.LocalTime> endTime = createTime("endTime", java.time.LocalTime.class);

    public final NumberPath<Integer> feePerPerson = createNumber("feePerPerson", Integer.class);

    public final StringPath hostId = createString("hostId");

    public final StringPath id = createString("id");

    public final DatePath<java.time.LocalDate> matchDate = createDate("matchDate", java.time.LocalDate.class);

    public final EnumPath<SkillLevel> maxSkillLevel = createEnum("maxSkillLevel", SkillLevel.class);

    public final EnumPath<SkillLevel> minSkillLevel = createEnum("minSkillLevel", SkillLevel.class);

    public final DateTimePath<java.time.LocalDateTime> recruitDeadline = createDateTime("recruitDeadline", java.time.LocalDateTime.class);

    public final EnumPath<RequiredGender> requiredGender = createEnum("requiredGender", RequiredGender.class);

    public final StringPath reservationId = createString("reservationId");

    public final EnumPath<SportType> sportType = createEnum("sportType", SportType.class);

    public final TimePath<java.time.LocalTime> startTime = createTime("startTime", java.time.LocalTime.class);

    public final EnumPath<MatchStatus> status = createEnum("status", MatchStatus.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QMatch(String variable) {
        super(Match.class, forVariable(variable));
    }

    public QMatch(Path<? extends Match> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatch(PathMetadata metadata) {
        super(Match.class, metadata);
    }

}

