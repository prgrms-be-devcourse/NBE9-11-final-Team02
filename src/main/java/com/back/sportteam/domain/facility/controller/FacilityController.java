package com.back.sportteam.domain.facility.controller;

import com.back.sportteam.domain.facility.dto.response.FacilityAvailableResponse;
import com.back.sportteam.domain.facility.service.FacilityAvailableService;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.global.response.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityAvailableService facilityAvailableService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<FacilityAvailableResponse>>> getAvailableFacilities(
            @RequestParam(required = false) SportType sportType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(facilityAvailableService.getAvailableFacilities(sportType, region, date, pageable))
        );
    }
}