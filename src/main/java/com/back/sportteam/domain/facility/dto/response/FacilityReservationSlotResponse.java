package com.back.sportteam.domain.facility.dto.response;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.entity.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record FacilityReservationSlotResponse(
        String slotId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        int price,
        SlotStatus slotStatus,
        String reservationId,
        ReservationStatus reservationStatus,
        long revenue
) {

    public static FacilityReservationSlotResponse of(
            FacilitySlot slot,
            Reservation reservation,
            long revenue
    ) {
        return new FacilityReservationSlotResponse(
                slot.getId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice(),
                slot.getStatus(),
                reservation == null ? null : reservation.getId(),
                reservation == null ? null : reservation.getStatus(),
                revenue
        );
    }
}
