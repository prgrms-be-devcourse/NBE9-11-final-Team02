package com.back.sportteam.domain.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "facility_slot_id", columnDefinition = "CHAR(36)", nullable = false)
    private String facilitySlotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    private Reservation(String facilitySlotId, LocalDateTime reservedAt) {
        this.id = UUID.randomUUID().toString();
        this.facilitySlotId = facilitySlotId;
        this.status = ReservationStatus.PENDING;
        this.reservedAt = reservedAt;
    }

    public static Reservation pending(String facilitySlotId, LocalDateTime reservedAt) {
        return new Reservation(facilitySlotId, reservedAt);
    }

    public void confirm() {
        if (status == ReservationStatus.CONFIRMED) {
            return;
        }
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed.");
        }

        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel(LocalDateTime cancelledAt) {
        if (status == ReservationStatus.CANCELLED) {
            return;
        }
        if (status == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("COMPLETED reservations cannot be cancelled.");
        }

        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public void complete() {
        if (status == ReservationStatus.COMPLETED) {
            return;
        }
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED reservations can be completed.");
        }

        this.status = ReservationStatus.COMPLETED;
    }
}
