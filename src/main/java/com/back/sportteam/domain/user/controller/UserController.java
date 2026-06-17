package com.back.sportteam.domain.user.controller;

import com.back.sportteam.domain.user.dto.request.UserProfileUpdateRequest;
import com.back.sportteam.domain.user.dto.response.UserProfileResponse;
import com.back.sportteam.domain.user.dto.response.UserProfileUpdateResponse;
import com.back.sportteam.domain.user.service.UserProfileService;
import com.back.sportteam.domain.user.service.UserProfileUpdateService;
import com.back.sportteam.domain.user.service.UserWithdrawService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity
                .ok(ApiResponse.ok(userProfileService.getMyProfile(userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileUpdateResponse>> updateMyProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return ResponseEntity
                .ok(ApiResponse.ok(userProfileUpdateService.updateMyProfile(userId, request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String accessToken = authorizationHeader.substring(7);
        userWithdrawService.withdraw(userId, accessToken);
        return ResponseEntity
                .ok(ApiResponse.ok());
    }
}