package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;

    @Transactional
    public FacilityResponse createFacility(String managerId, FacilityCreateRequest request) {
        Facility facility = Facility.create(
                managerId,
                request.name(),
                request.address(),
                request.phone(),
                request.description(),
                request.slotDurationMinutes(),
                request.maxSlots(),
                request.slotOpenAt(),
                request.sportTypes(),
                request.amenities(),
                request.imageUrls()
        );
        return FacilityResponse.from(facilityRepository.save(facility));
    }
}
