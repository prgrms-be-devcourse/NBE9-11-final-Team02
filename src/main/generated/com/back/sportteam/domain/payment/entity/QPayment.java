package com.back.sportteam.domain.payment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPayment is a Querydsl query type for Payment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPayment extends EntityPathBase<Payment> {

    private static final long serialVersionUID = 380815382L;

    public static final QPayment payment = new QPayment("payment");

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final StringPath facilitySlotId = createString("facilitySlotId");

    public final StringPath id = createString("id");

    public final StringPath matchId = createString("matchId");

    public final StringPath merchantUid = createString("merchantUid");

    public final DateTimePath<java.time.LocalDateTime> paidAt = createDateTime("paidAt", java.time.LocalDateTime.class);

    public final StringPath participantId = createString("participantId");

    public final EnumPath<PaymentType> paymentType = createEnum("paymentType", PaymentType.class);

    public final EnumPath<PaymentProvider> pgProvider = createEnum("pgProvider", PaymentProvider.class);

    public final StringPath pgTransactionId = createString("pgTransactionId");

    public final NumberPath<Integer> refundedAmount = createNumber("refundedAmount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> refundedAt = createDateTime("refundedAt", java.time.LocalDateTime.class);

    public final EnumPath<PaymentStatus> status = createEnum("status", PaymentStatus.class);

    public final StringPath userId = createString("userId");

    public QPayment(String variable) {
        super(Payment.class, forVariable(variable));
    }

    public QPayment(Path<? extends Payment> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPayment(PathMetadata metadata) {
        super(Payment.class, metadata);
    }

}

