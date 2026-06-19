package com.back.sportteam.domain.match.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchParticipant is a Querydsl query type for MatchParticipant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchParticipant extends EntityPathBase<MatchParticipant> {

    private static final long serialVersionUID = 286037533L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchParticipant matchParticipant = new QMatchParticipant("matchParticipant");

    public final StringPath id = createString("id");

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final QMatch match;

    public final DateTimePath<java.time.LocalDateTime> paymentDeadline = createDateTime("paymentDeadline", java.time.LocalDateTime.class);

    public final EnumPath<MatchParticipantRole> role = createEnum("role", MatchParticipantRole.class);

    public final EnumPath<MatchParticipantStatus> status = createEnum("status", MatchParticipantStatus.class);

    public final StringPath userId = createString("userId");

    public QMatchParticipant(String variable) {
        this(MatchParticipant.class, forVariable(variable), INITS);
    }

    public QMatchParticipant(Path<? extends MatchParticipant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchParticipant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchParticipant(PathMetadata metadata, PathInits inits) {
        this(MatchParticipant.class, metadata, inits);
    }

    public QMatchParticipant(Class<? extends MatchParticipant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QMatch(forProperty("match")) : null;
    }

}

