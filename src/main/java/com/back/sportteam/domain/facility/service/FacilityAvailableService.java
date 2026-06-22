package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.response.FacilityAvailableResponse;
import com.back.sportteam.domain.facility.repository.FacilityQueryRepository;
import com.back.sportteam.domain.match.entity.SportType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityAvailableService {

    private final FacilityQueryRepository facilityQueryRepository;

    @Cacheable(
            cacheNames = "facilities:available",
            key = "#sportType + ':' + #region + ':' + #date + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
            unless = "#result.isEmpty()"
    )
    public Page<FacilityAvailableResponse> getAvailableFacilities(
            SportType sportType,
            String region,
            LocalDate date,
            Pageable pageable
    ) {
        return facilityQueryRepository.findAvailable(sportType, region, date, pageable)
                .map(FacilityAvailableResponse::from);
    }
}