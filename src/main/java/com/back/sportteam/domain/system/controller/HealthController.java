package com.back.sportteam.domain.system.controller;

import com.back.sportteam.domain.system.dto.response.HealthResponse;
import com.back.sportteam.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> getHealth() {
        return ResponseEntity.ok(ApiResponse.ok(new HealthResponse("UP")));
    }
}
