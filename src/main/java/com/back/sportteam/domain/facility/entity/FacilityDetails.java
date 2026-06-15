package com.back.sportteam.domain.facility.entity;

import com.back.sportteam.domain.match.entity.SportType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder
public record FacilityDetails(
        String phone,
        String description,
        int capacity,
        int slotDurationMinutes,
        int defaultWeekdayPrice,
        int defaultWeekendPrice,
        LocalDateTime slotOpenAt,
        Set<SportType> sportTypes,
        Set<Amenity> amenities,
        List<String> imageUrls
) {
}
