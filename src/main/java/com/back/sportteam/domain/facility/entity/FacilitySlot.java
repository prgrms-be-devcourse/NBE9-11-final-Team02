package com.back.sportteam.domain.facility.entity;

import com.back.sportteam.global.util.TimeUtils;
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
import java.util.UUID;

@Getter
@Entity
@Table(name = "facility_slots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilitySlot {

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
    @Column(name = "status", nullable = false, length = 20)
    private SlotStatus status;

    @Column(name = "pending_until")
    private LocalDateTime pendingUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private FacilitySlot(String facilityId, LocalDate slotDate, LocalTime startTime,
                         LocalTime endTime, int price) {
        LocalDateTime now = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
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

    public void update(int price, SlotStatus status) {
        this.price = price;
        this.status = status;
    }

    public void holdUntil(LocalDateTime pendingUntil) {
        if (status != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE slots can be held.");
        }

        this.status = SlotStatus.PENDING;
        this.pendingUntil = pendingUntil;
    }

    public void reserve() {
        if (status == SlotStatus.RESERVED) {
            return;
        }
        if (status != SlotStatus.PENDING && status != SlotStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE or PENDING slots can be reserved.");
        }

        this.status = SlotStatus.RESERVED;
        this.pendingUntil = null;
    }

    public void release() {
        if (status == SlotStatus.AVAILABLE) {
            return;
        }
        if (status == SlotStatus.CLOSED) {
            throw new IllegalStateException("CLOSED slots cannot be released.");
        }

        this.status = SlotStatus.AVAILABLE;
        this.pendingUntil = null;
    }

    public boolean isReservable() {
        return this.status == SlotStatus.AVAILABLE;
    }

    public void holdUntil(LocalDateTime pendingUntil) {
        this.status = SlotStatus.PENDING;
        this.pendingUntil = pendingUntil;
    }

    public void reserve() {
        this.status = SlotStatus.RESERVED;
        this.pendingUntil = null;
    }

    public void release() {
        this.status = SlotStatus.AVAILABLE;
        this.pendingUntil = null;
    }

    public boolean isManagerEditable() {
        return this.status == SlotStatus.AVAILABLE || this.status == SlotStatus.CLOSED;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
    }
}
