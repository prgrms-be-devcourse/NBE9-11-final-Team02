package com.back.sportteam.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 334771206L;

    public static final QUser user = new QUser("user");

    public final StringPath activeRegion = createString("activeRegion");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final StringPath id = createString("id");

    public final NumberPath<java.math.BigDecimal> mannerRatingSum = createNumber("mannerRatingSum", java.math.BigDecimal.class);

    public final NumberPath<Integer> mannerReviewCount = createNumber("mannerReviewCount", Integer.class);

    public final NumberPath<Double> mannerScore = createNumber("mannerScore", Double.class);

    public final StringPath nickname = createString("nickname");

    public final StringPath passwordHash = createString("passwordHash");

    public final StringPath position = createString("position");

    public final StringPath preferredSport = createString("preferredSport");

    public final StringPath profileImg = createString("profileImg");

    public final EnumPath<com.back.sportteam.domain.auth.provider.AuthProvider> provider = createEnum("provider", com.back.sportteam.domain.auth.provider.AuthProvider.class);

    public final StringPath providerId = createString("providerId");

    public final EnumPath<UserRole> role = createEnum("role", UserRole.class);

    public final NumberPath<Double> skillScore = createNumber("skillScore", Double.class);

    public final ListPath<UserSportStat, QUserSportStat> sportStats = this.<UserSportStat, QUserSportStat>createList("sportStats", UserSportStat.class, QUserSportStat.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

