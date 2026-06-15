package com.back.sportteam.domain.facility.dto.response;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record FacilitySlotResponse(
        String id,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        int price,
        SlotStatus status
) {
    public static FacilitySlotResponse from(FacilitySlot slot) {
        return new FacilitySlotResponse(
                slot.getId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice(),
                slot.getStatus()
        );
    }
}
