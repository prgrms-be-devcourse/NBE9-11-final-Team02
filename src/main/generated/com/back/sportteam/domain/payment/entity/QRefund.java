package com.back.sportteam.domain.payment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRefund is a Querydsl query type for Refund
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRefund extends EntityPathBase<Refund> {

    private static final long serialVersionUID = -1451341976L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRefund refund = new QRefund("refund");

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    public final StringPath failureReason = createString("failureReason");

    public final StringPath id = createString("id");

    public final DateTimePath<java.time.LocalDateTime> lastAttemptedAt = createDateTime("lastAttemptedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> nextRetryAt = createDateTime("nextRetryAt", java.time.LocalDateTime.class);

    public final QPayment payment;

    public final NumberPath<Integer> pendingFlag = createNumber("pendingFlag", Integer.class);

    public final StringPath reason = createString("reason");

    public final DateTimePath<java.time.LocalDateTime> requestedAt = createDateTime("requestedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> retryCount = createNumber("retryCount", Integer.class);

    public final EnumPath<RefundStatus> status = createEnum("status", RefundStatus.class);

    public QRefund(String variable) {
        this(Refund.class, forVariable(variable), INITS);
    }

    public QRefund(Path<? extends Refund> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRefund(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRefund(PathMetadata metadata, PathInits inits) {
        this(Refund.class, metadata, inits);
    }

    public QRefund(Class<? extends Refund> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.payment = inits.isInitialized("payment") ? new QPayment(forProperty("payment")) : null;
    }

}

