package com.back.sportteam.domain.auth.controller;

import com.back.sportteam.domain.auth.dto.request.LoginRequest;
import com.back.sportteam.domain.auth.dto.request.SignupRequest;
import com.back.sportteam.domain.auth.dto.response.LoginResponse;
import com.back.sportteam.domain.auth.dto.response.SignupResponse;
import com.back.sportteam.domain.auth.service.AuthLoginService;
import com.back.sportteam.domain.auth.service.AuthSignupService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthSignupService authSignupService;
    private final AuthLoginService authLoginService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authSignupService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok(authLoginService.login(request, response)));
    }
}