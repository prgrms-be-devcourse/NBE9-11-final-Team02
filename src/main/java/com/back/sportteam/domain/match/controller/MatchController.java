package com.back.sportteam.domain.match.controller;

import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.dto.response.MatchDetailResponse;
import com.back.sportteam.domain.match.dto.response.MatchSummaryResponse;
import com.back.sportteam.domain.match.service.MatchService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<ApiResponse<MatchCreateResponse>> createMatch(
            @RequestHeader("X-USER-ID") @NotBlank(message = "사용자 ID는 필수입니다.") String hostId,
            @Valid @RequestBody MatchCreateRequest request
    ) {
        MatchCreateResponse response = matchService.createMatch(hostId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> getMatches() {
        List<MatchSummaryResponse> response = matchService.getMatches();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponse<MatchDetailResponse>> getMatch(@PathVariable String matchId) {
        MatchDetailResponse response = matchService.getMatch(matchId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
