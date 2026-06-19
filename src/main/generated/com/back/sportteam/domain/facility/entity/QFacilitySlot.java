package com.back.sportteam.domain.facility.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFacilitySlot is a Querydsl query type for FacilitySlot
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFacilitySlot extends EntityPathBase<FacilitySlot> {

    private static final long serialVersionUID = 1423328724L;

    public static final QFacilitySlot facilitySlot = new QFacilitySlot("facilitySlot");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final TimePath<java.time.LocalTime> endTime = createTime("endTime", java.time.LocalTime.class);

    public final StringPath facilityId = createString("facilityId");

    public final StringPath id = createString("id");

    public final DateTimePath<java.time.LocalDateTime> pendingUntil = createDateTime("pendingUntil", java.time.LocalDateTime.class);

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final DatePath<java.time.LocalDate> slotDate = createDate("slotDate", java.time.LocalDate.class);

    public final TimePath<java.time.LocalTime> startTime = createTime("startTime", java.time.LocalTime.class);

    public final EnumPath<SlotStatus> status = createEnum("status", SlotStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QFacilitySlot(String variable) {
        super(FacilitySlot.class, forVariable(variable));
    }

    public QFacilitySlot(Path<? extends FacilitySlot> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFacilitySlot(PathMetadata metadata) {
        super(FacilitySlot.class, metadata);
    }

}

