package com.back.sportteam.domain.user.controller;

import com.back.sportteam.domain.user.dto.request.UserProfileUpdateRequest;
import com.back.sportteam.domain.user.dto.response.UserProfileResponse;
import com.back.sportteam.domain.user.dto.response.UserProfileUpdateResponse;
import com.back.sportteam.domain.user.service.UserProfileService;
import com.back.sportteam.domain.user.service.UserProfileUpdateService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final UserProfileUpdateService userProfileUpdateService;

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
}
