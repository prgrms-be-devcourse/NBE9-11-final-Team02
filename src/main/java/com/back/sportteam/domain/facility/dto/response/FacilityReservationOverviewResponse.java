package com.back.sportteam.domain.facility.dto.response;

import java.time.LocalDate;
import java.util.List;

public record FacilityReservationOverviewResponse(
        String facilityId,
        LocalDate fromDate,
        LocalDate toDate,
        long totalSlots,
        long reservedSlots,
        long availableSlots,
        long totalRevenue,
        List<FacilityReservationSlotResponse> slots
) {
}
