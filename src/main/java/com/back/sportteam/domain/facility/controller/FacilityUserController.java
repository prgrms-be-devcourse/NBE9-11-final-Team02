package com.back.sportteam.domain.facility.controller;

import com.back.sportteam.domain.facility.dto.response.FacilitySlotResponse;
import com.back.sportteam.domain.facility.service.FacilityService;
import com.back.sportteam.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/facilities")
public class FacilityUserController {

    private final FacilityService facilityService;

    @GetMapping("/{facilityId}/slots")
    public ResponseEntity<ApiResponse<List<FacilitySlotResponse>>> getSlotsByDate(
            @PathVariable String facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<FacilitySlotResponse> response = facilityService.getSlotsByDate(facilityId, date);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
