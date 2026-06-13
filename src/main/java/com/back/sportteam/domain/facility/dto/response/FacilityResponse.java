package com.back.sportteam.domain.facility.dto.response;


import com.back.sportteam.domain.facility.entity.Amenity;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.match.entity.SportType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record FacilityResponse(
        String id,
        String name,
        String address,
        String phone,
        String description,
        int slotDurationMinutes,
        int maxSlots,
        LocalDateTime slotOpenAt,
        FacilityStatus status,
        Set<SportType> sportTypes,
        Set<Amenity> amenities,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static FacilityResponse from(Facility facility) {
        return new FacilityResponse(
                facility.getId(),
                facility.getName(),
                facility.getAddress(),
                facility.getPhone(),
                facility.getDescription(),
                facility.getSlotDurationMinutes(),
                facility.getMaxSlots(),
                facility.getSlotOpenAt(),
                facility.getStatus(),
                facility.getSportTypes(),
                facility.getAmenities(),
                facility.getImageUrls(),
                facility.getCreatedAt()
        );
    }
}
