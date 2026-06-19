package com.back.sportteam.domain.payment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPaymentWebhookEvent is a Querydsl query type for PaymentWebhookEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPaymentWebhookEvent extends EntityPathBase<PaymentWebhookEvent> {

    private static final long serialVersionUID = 1218292121L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPaymentWebhookEvent paymentWebhookEvent = new QPaymentWebhookEvent("paymentWebhookEvent");

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath eventId = createString("eventId");

    public final EnumPath<PaymentWebhookEventType> eventType = createEnum("eventType", PaymentWebhookEventType.class);

    public final EnumPath<PaymentStatus> finalPaymentStatus = createEnum("finalPaymentStatus", PaymentStatus.class);

    public final StringPath id = createString("id");

    public final QPayment payment;

    public final EnumPath<PaymentStatus> paymentStatus = createEnum("paymentStatus", PaymentStatus.class);

    public final StringPath pgTransactionId = createString("pgTransactionId");

    public final EnumPath<PaymentStatus> previousPaymentStatus = createEnum("previousPaymentStatus", PaymentStatus.class);

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final EnumPath<PaymentWebhookProcessingResult> processingResult = createEnum("processingResult", PaymentWebhookProcessingResult.class);

    public final StringPath resultReason = createString("resultReason");

    public QPaymentWebhookEvent(String variable) {
        this(PaymentWebhookEvent.class, forVariable(variable), INITS);
    }

    public QPaymentWebhookEvent(Path<? extends PaymentWebhookEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPaymentWebhookEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPaymentWebhookEvent(PathMetadata metadata, PathInits inits) {
        this(PaymentWebhookEvent.class, metadata, inits);
    }

    public QPaymentWebhookEvent(Class<? extends PaymentWebhookEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.payment = inits.isInitialized("payment") ? new QPayment(forProperty("payment")) : null;
    }

}

