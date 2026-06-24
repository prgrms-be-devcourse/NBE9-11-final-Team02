package com.back.sportteam.domain.user.controller;

import com.back.sportteam.domain.user.dto.request.SportStatRegisterRequest;
import com.back.sportteam.domain.user.dto.request.UserProfileUpdateRequest;
import com.back.sportteam.domain.user.dto.response.SportStatRegisterResponse;
import com.back.sportteam.domain.user.dto.response.SportStatResponse;
import com.back.sportteam.domain.user.dto.response.UserProfileResponse;
import com.back.sportteam.domain.user.dto.response.UserProfileUpdateResponse;
import com.back.sportteam.domain.user.service.UserProfileService;
import com.back.sportteam.domain.user.service.UserProfileUpdateService;
import com.back.sportteam.domain.user.service.UserSportStatService;
import com.back.sportteam.domain.user.service.UserWithdrawService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final UserProfileUpdateService userProfileUpdateService;
    private final UserWithdrawService userWithdrawService;
    private final UserSportStatService userSportStatService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity
                .ok(ApiResponse.ok(userProfileService.getMyProfile(userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileUpdateResponse>> updateMyProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return ResponseEntity
                .ok(ApiResponse.ok(userProfileUpdateService.updateMyProfile(userId, request)));
    }

    @GetMapping("/me/sport-stats")
    public ResponseEntity<ApiResponse<List<SportStatResponse>>> getSportStats(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userSportStatService.getSportStats(userId)));
    }

    @PostMapping("/me/sport-stats")
    public ResponseEntity<ApiResponse<SportStatRegisterResponse>> registerSportStats(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SportStatRegisterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userSportStatService.registerSportStats(userId, request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal String userId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String accessToken = authorizationHeader.substring(7);
        userWithdrawService.withdraw(userId, accessToken);
        return ResponseEntity
                .ok(ApiResponse.ok());
    }
}