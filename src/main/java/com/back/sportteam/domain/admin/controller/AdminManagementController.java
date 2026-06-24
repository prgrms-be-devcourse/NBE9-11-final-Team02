package com.back.sportteam.domain.admin.controller;

import com.back.sportteam.domain.admin.dto.request.AdminUserRestrictionRequest;
import com.back.sportteam.domain.admin.dto.response.AdminFacilityResponse;
import com.back.sportteam.domain.admin.dto.response.AdminUserResponse;
import com.back.sportteam.domain.admin.service.AdminManagementService;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @GetMapping("/facilities")
    public ResponseEntity<ApiResponse<Page<AdminFacilityResponse>>> getFacilities(
            @RequestParam(required = false) FacilityStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminManagementService.getFacilities(status, pageable)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminManagementService.getUsers(role, pageable)));
    }

    @GetMapping("/users/blacklist")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getRestrictedUsers(
            @PageableDefault(size = 20, sort = "restrictedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminManagementService.getRestrictedUsers(pageable)));
    }

    @GetMapping("/users/blacklist/candidates")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getBlacklistCandidates(
            @RequestParam(required = false) BigDecimal maxMannerScore,
            @RequestParam(required = false) Integer minReviewCount,
            @PageableDefault(size = 20, sort = "mannerScore", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminManagementService.getBlacklistCandidates(maxMannerScore, minReviewCount, pageable)
        ));
    }

    @PatchMapping("/users/{userId}/restriction")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRestriction(
            @PathVariable String userId,
            @Valid @RequestBody AdminUserRestrictionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminManagementService.updateUserRestriction(userId, request.restricted(), request.reason())
        ));
    }
}
