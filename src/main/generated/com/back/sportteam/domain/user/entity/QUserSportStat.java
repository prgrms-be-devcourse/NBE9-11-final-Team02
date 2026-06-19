package com.back.sportteam.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserSportStat is a Querydsl query type for UserSportStat
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserSportStat extends EntityPathBase<UserSportStat> {

    private static final long serialVersionUID = 14211714L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserSportStat userSportStat = new QUserSportStat("userSportStat");

    public final StringPath id = createString("id");

    public final StringPath position = createString("position");

    public final NumberPath<Integer> reviewCount = createNumber("reviewCount", Integer.class);

    public final EnumPath<SelfReportedLevel> selfReportedLevel = createEnum("selfReportedLevel", SelfReportedLevel.class);

    public final NumberPath<java.math.BigDecimal> skillRating = createNumber("skillRating", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> skillRatingSum = createNumber("skillRatingSum", java.math.BigDecimal.class);

    public final EnumPath<com.back.sportteam.domain.match.entity.SportType> sportType = createEnum("sportType", com.back.sportteam.domain.match.entity.SportType.class);

    public final QUser user;

    public QUserSportStat(String variable) {
        this(UserSportStat.class, forVariable(variable), INITS);
    }

    public QUserSportStat(Path<? extends UserSportStat> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserSportStat(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserSportStat(PathMetadata metadata, PathInits inits) {
        this(UserSportStat.class, metadata, inits);
    }

    public QUserSportStat(Class<? extends UserSportStat> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

