package com.back.sportteam.domain.facility.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Entity
@Table(name = "facility_slots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilitySlot {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "facility_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String facilityId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "price", nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SlotStatus status;

    @Column(name = "pending_until")
    private LocalDateTime pendingUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private FacilitySlot(String facilityId, LocalDate slotDate, LocalTime startTime,
                         LocalTime endTime, int price) {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        this.id = UUID.randomUUID().toString();
        this.facilityId = facilityId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = SlotStatus.AVAILABLE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static FacilitySlot create(String facilityId, LocalDate slotDate,
                                      LocalTime startTime, LocalTime endTime, int price) {
        return new FacilitySlot(facilityId, slotDate, startTime, endTime, price);
    }

    public boolean isReservable() {
        return this.status == SlotStatus.AVAILABLE;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now(SERVICE_ZONE);
    }
}
