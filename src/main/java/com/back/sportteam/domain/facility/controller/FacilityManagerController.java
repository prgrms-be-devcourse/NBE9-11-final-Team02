package com.back.sportteam.domain.facility.controller;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.request.FacilityUpdateRequest;
import com.back.sportteam.domain.facility.dto.request.SlotSetupRequest;
import com.back.sportteam.domain.facility.dto.request.SlotUpdateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.dto.response.FacilityReservationOverviewResponse;
import com.back.sportteam.domain.facility.dto.response.FacilitySummaryResponse;
import com.back.sportteam.domain.facility.dto.response.FacilitySlotResponse;
import com.back.sportteam.domain.facility.service.FacilityService;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/facilities")
public class FacilityManagerController {

    private final FacilityService facilityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FacilitySummaryResponse>>> getMyFacilities(
            @AuthenticationPrincipal @NotBlank String managerId
    ) {
        List<FacilitySummaryResponse> response = facilityService.getMyFacilities(requireManagerId(managerId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{facilityId}/reservations")
    public ResponseEntity<ApiResponse<FacilityReservationOverviewResponse>> getReservations(
            @AuthenticationPrincipal @NotBlank String managerId,
            @PathVariable String facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        FacilityReservationOverviewResponse response = facilityService.getReservations(
                requireManagerId(managerId),
                facilityId,
                fromDate,
                toDate
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FacilityResponse>> createFacility(
            @AuthenticationPrincipal @NotBlank String managerId,
            @Valid @RequestBody FacilityCreateRequest request
    ) {
        FacilityResponse response = facilityService.createFacility(requireManagerId(managerId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PatchMapping("/{facilityId}")
    public ResponseEntity<ApiResponse<FacilityResponse>> updateFacility(
            @AuthenticationPrincipal @NotBlank String managerId,
            @PathVariable String facilityId,
            @Valid @RequestBody FacilityUpdateRequest request
    ) {
        FacilityResponse response = facilityService.updateFacility(requireManagerId(managerId), facilityId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{facilityId}")
    public ResponseEntity<ApiResponse<Void>> deleteFacility(
            @AuthenticationPrincipal @NotBlank String managerId,
            @PathVariable String facilityId
    ) {
        facilityService.deleteFacility(requireManagerId(managerId), facilityId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{facilityId}/images")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @AuthenticationPrincipal @NotBlank String managerId,
            @PathVariable String facilityId,
            @RequestParam @NotBlank String imageUrl
    ) {
        facilityService.deleteImage(requireManagerId(managerId), facilityId, imageUrl);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/{facilityId}/slots")
    public ResponseEntity<ApiResponse<List<FacilitySlotResponse>>> setupSlots(
            @AuthenticationPrincipal @NotBlank String managerId,
            @PathVariable String facilityId,
            @Valid @RequestBody SlotSetupRequest request
    ) {
        List<FacilitySlotResponse> response = facilityService.setupSlots(requireManagerId(managerId), facilityId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PatchMapping("/{facilityId}/slots/{slotId}")
    public ResponseEntity<ApiResponse<FacilitySlotResponse>> updateSlot(
            @AuthenticationPrincipal @NotBlank String managerId,
            @PathVariable String facilityId,
            @PathVariable String slotId,
            @Valid @RequestBody SlotUpdateRequest request
    ) {
        FacilitySlotResponse response = facilityService.updateSlot(requireManagerId(managerId), facilityId, slotId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private String requireManagerId(String managerId) {
        if (!StringUtils.hasText(managerId)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        return managerId;
    }
}
