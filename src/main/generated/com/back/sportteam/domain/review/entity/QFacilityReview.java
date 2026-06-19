package com.back.sportteam.domain.review.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFacilityReview is a Querydsl query type for FacilityReview
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFacilityReview extends EntityPathBase<FacilityReview> {

    private static final long serialVersionUID = -1492528797L;

    public static final QFacilityReview facilityReview = new QFacilityReview("facilityReview");

    public final StringPath comment = createString("comment");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath facilityId = createString("facilityId");

    public final StringPath id = createString("id");

    public final StringPath matchId = createString("matchId");

    public final NumberPath<java.math.BigDecimal> rating = createNumber("rating", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath userId = createString("userId");

    public QFacilityReview(String variable) {
        super(FacilityReview.class, forVariable(variable));
    }

    public QFacilityReview(Path<? extends FacilityReview> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFacilityReview(PathMetadata metadata) {
        super(FacilityReview.class, metadata);
    }

}

