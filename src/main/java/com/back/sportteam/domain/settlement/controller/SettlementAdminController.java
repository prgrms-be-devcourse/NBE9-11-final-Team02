package com.back.sportteam.domain.settlement.controller;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.settlement.dto.response.SettlementItemResponse;
import com.back.sportteam.domain.settlement.dto.response.SettlementSummaryResponse;
import com.back.sportteam.domain.settlement.service.SettlementAdminService;
import com.back.sportteam.global.response.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/settlements")
public class SettlementAdminController {

    private final SettlementAdminService settlementAdminService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SettlementSummaryResponse>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                settlementAdminService.getSummary(from, to)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SettlementItemResponse>>> getSettlements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) SportType sportType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                settlementAdminService.getSettlements(from, to, sportType, pageable)
        ));
    }
}
