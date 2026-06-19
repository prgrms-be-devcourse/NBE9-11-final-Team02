package com.back.sportteam.domain.system.controller;

import com.back.sportteam.domain.system.dto.response.WaitingQueueTokenResponse;
import com.back.sportteam.global.response.ApiResponse;
import com.back.sportteam.infra.redis.queue.WaitingQueueService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queue")
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;

    @PostMapping("/facility-slots/{facilitySlotId}/tokens")
    public ResponseEntity<ApiResponse<WaitingQueueTokenResponse>> issueToken(
            @PathVariable String facilitySlotId,
            @RequestHeader("X-USER-ID") @NotBlank(message = "사용자 ID는 필수입니다.") String userId
    ) {
        WaitingQueueTokenResponse response = waitingQueueService.issueToken(facilitySlotId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/tokens/{token}")
    public ResponseEntity<ApiResponse<WaitingQueueTokenResponse>> getStatus(
            @PathVariable String token
    ) {
        WaitingQueueTokenResponse response = waitingQueueService.getStatus(token);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
