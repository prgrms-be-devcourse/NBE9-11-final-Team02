package com.back.sportteam.domain.match.controller;

import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.service.MatchService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Profile("dev")
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchLockTestController {

    private final MatchService matchService;

    // dev 환경에서 k6 비교 실험을 하기 위한 비관적 락 전용 엔드포인트
    @PostMapping("/{matchId}/participants/pessimistic-lock")
    public ResponseEntity<ApiResponse<MatchParticipantResponse>> joinMatchWithPessimisticLock(
            @PathVariable String matchId,
            @RequestHeader("X-USER-ID") @NotBlank(message = "사용자 ID는 필수입니다.") String userId
    ) {
        MatchParticipantResponse response = matchService.joinMatchWithPessimisticLock(matchId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
