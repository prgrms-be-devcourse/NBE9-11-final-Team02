package com.back.sportteam.domain.facility.dto.request;

import com.back.sportteam.domain.facility.entity.Amenity;
import com.back.sportteam.domain.match.entity.SportType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record FacilityUpdateRequest(
        String phone,

        String description,

        @NotNull
        @Min(value = 30, message = "슬롯 단위는 최소 30분입니다.")
        Integer slotDurationMinutes,

        LocalDateTime slotOpenAt,

        Set<SportType> sportTypes,

        Set<Amenity> amenities,

        List<String> imageUrls
) {
}
