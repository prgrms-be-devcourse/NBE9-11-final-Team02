package com.back.sportteam.domain.admin.dto.response;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.match.entity.SportType;
import java.time.LocalDateTime;
import java.util.Set;

public record AdminFacilityResponse(
        String facilityId,
        String managerId,
        String name,
        String address,
        FacilityStatus status,
        Set<SportType> sportTypes,
        LocalDateTime createdAt
) {

    public static AdminFacilityResponse from(Facility facility) {
        return new AdminFacilityResponse(
                facility.getId(),
                facility.getManagerId(),
                facility.getName(),
                facility.getAddress(),
                facility.getStatus(),
                Set.copyOf(facility.getSportTypes()),
                facility.getCreatedAt()
        );
    }
}
