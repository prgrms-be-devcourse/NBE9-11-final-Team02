package com.back.sportteam.domain.facility.dto.response;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.match.entity.SportType;

import java.util.Set;

public record FacilitySummaryResponse(
        String id,
        String thumbnailUrl,
        String name,
        String address,
        FacilityStatus status,
        Set<SportType> sportTypes
) {
    public static FacilitySummaryResponse from(Facility facility) {
        return new FacilitySummaryResponse(
                facility.getId(),
                facility.getImageUrls().isEmpty() ? null : facility.getImageUrls().get(0),
                facility.getName(),
                facility.getAddress(),
                facility.getStatus(),
                facility.getSportTypes()
        );
    }
}
