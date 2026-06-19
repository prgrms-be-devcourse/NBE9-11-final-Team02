package com.back.sportteam.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNotification is a Querydsl query type for Notification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotification extends EntityPathBase<Notification> {

    private static final long serialVersionUID = -624431034L;

    public static final QNotification notification = new QNotification("notification");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath id = createString("id");

    public final StringPath message = createString("message");

    public final DateTimePath<java.time.LocalDateTime> readAt = createDateTime("readAt", java.time.LocalDateTime.class);

    public final StringPath referenceId = createString("referenceId");

    public final StringPath referenceType = createString("referenceType");

    public final StringPath title = createString("title");

    public final EnumPath<NotificationType> type = createEnum("type", NotificationType.class);

    public final StringPath userId = createString("userId");

    public QNotification(String variable) {
        super(Notification.class, forVariable(variable));
    }

    public QNotification(Path<? extends Notification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNotification(PathMetadata metadata) {
        super(Notification.class, metadata);
    }

}

