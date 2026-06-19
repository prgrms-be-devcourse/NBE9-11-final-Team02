package com.back.sportteam.domain.review.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QParticipantReview is a Querydsl query type for ParticipantReview
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QParticipantReview extends EntityPathBase<ParticipantReview> {

    private static final long serialVersionUID = 1582419427L;

    public static final QParticipantReview participantReview = new QParticipantReview("participantReview");

    public final StringPath comment = createString("comment");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath id = createString("id");

    public final NumberPath<java.math.BigDecimal> mannerRating = createNumber("mannerRating", java.math.BigDecimal.class);

    public final StringPath matchId = createString("matchId");

    public final StringPath revieweeId = createString("revieweeId");

    public final StringPath reviewerId = createString("reviewerId");

    public final NumberPath<java.math.BigDecimal> skillRating = createNumber("skillRating", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QParticipantReview(String variable) {
        super(ParticipantReview.class, forVariable(variable));
    }

    public QParticipantReview(Path<? extends ParticipantReview> path) {
        super(path.getType(), path.getMetadata());
    }

    public QParticipantReview(PathMetadata metadata) {
        super(ParticipantReview.class, metadata);
    }

}

