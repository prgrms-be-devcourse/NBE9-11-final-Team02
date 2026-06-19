package com.back.sportteam.domain.facility.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFacility is a Querydsl query type for Facility
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFacility extends EntityPathBase<Facility> {

    private static final long serialVersionUID = -1953287498L;

    public static final QFacility facility = new QFacility("facility");

    public final StringPath address = createString("address");

    public final SetPath<Amenity, EnumPath<Amenity>> amenities = this.<Amenity, EnumPath<Amenity>>createSet("amenities", Amenity.class, EnumPath.class, PathInits.DIRECT2);

    public final NumberPath<Integer> capacity = createNumber("capacity", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> defaultWeekdayPrice = createNumber("defaultWeekdayPrice", Integer.class);

    public final NumberPath<Integer> defaultWeekendPrice = createNumber("defaultWeekendPrice", Integer.class);

    public final StringPath description = createString("description");

    public final StringPath id = createString("id");

    public final ListPath<String, StringPath> imageUrls = this.<String, StringPath>createList("imageUrls", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath managerId = createString("managerId");

    public final StringPath name = createString("name");

    public final StringPath phone = createString("phone");

    public final NumberPath<java.math.BigDecimal> ratingAvg = createNumber("ratingAvg", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> ratingSum = createNumber("ratingSum", java.math.BigDecimal.class);

    public final NumberPath<Integer> reviewCount = createNumber("reviewCount", Integer.class);

    public final NumberPath<Integer> slotDurationMinutes = createNumber("slotDurationMinutes", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> slotOpenAt = createDateTime("slotOpenAt", java.time.LocalDateTime.class);

    public final SetPath<com.back.sportteam.domain.match.entity.SportType, EnumPath<com.back.sportteam.domain.match.entity.SportType>> sportTypes = this.<com.back.sportteam.domain.match.entity.SportType, EnumPath<com.back.sportteam.domain.match.entity.SportType>>createSet("sportTypes", com.back.sportteam.domain.match.entity.SportType.class, EnumPath.class, PathInits.DIRECT2);

    public final EnumPath<FacilityStatus> status = createEnum("status", FacilityStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QFacility(String variable) {
        super(Facility.class, forVariable(variable));
    }

    public QFacility(Path<? extends Facility> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFacility(PathMetadata metadata) {
        super(Facility.class, metadata);
    }

}

