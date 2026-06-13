package com.back.sportteam.domain.facility.controller;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.request.FacilityUpdateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.service.FacilityService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    @PostMapping
    public ResponseEntity<ApiResponse<FacilityResponse>> createFacility(
            @RequestHeader("X-USER-ID") @NotBlank String managerId,
            @Valid @RequestBody FacilityCreateRequest request
    ) {
        FacilityResponse response = facilityService.createFacility(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PatchMapping("/{facilityId}")
    public ResponseEntity<ApiResponse<FacilityResponse>> updateFacility(
            @RequestHeader("X-USER-ID") @NotBlank String managerId,
            @PathVariable String facilityId,
            @Valid @RequestBody FacilityUpdateRequest request
    ) {
        FacilityResponse response = facilityService.updateFacility(managerId, facilityId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
