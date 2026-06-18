package com.back.sportteam.domain.match.controller;

import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.request.MatchSearchCondition;
import com.back.sportteam.domain.match.dto.request.MatchSortType;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.dto.response.MatchDetailResponse;
import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.dto.response.MatchSummaryResponse;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.service.MatchJoinFacade;
import com.back.sportteam.domain.match.service.MatchService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchJoinFacade matchJoinFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<MatchCreateResponse>> createMatch(
            @AuthenticationPrincipal String hostId,
            @Valid @RequestBody MatchCreateRequest request
    ) {
        MatchCreateResponse response = matchService.createMatch(hostId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MatchSummaryResponse>>> getMatches(
            @RequestParam(required = false) SportType sportType,
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) SkillLevel minSkillLevel,
            @RequestParam(required = false) SkillLevel maxSkillLevel,
            @RequestParam(required = false) RequiredGender requiredGender,
            @RequestParam(defaultValue = "LATEST") MatchSortType sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        MatchSearchCondition condition = new MatchSearchCondition(
                sportType,
                status,
                minSkillLevel,
                maxSkillLevel,
                requiredGender,
                sort,
                page,
                size
        );
        Page<MatchSummaryResponse> response = matchService.getMatches(condition);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponse<MatchDetailResponse>> getMatch(@PathVariable String matchId) {
        MatchDetailResponse response = matchService.getMatch(matchId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{matchId}/participants")
    public ResponseEntity<ApiResponse<List<MatchParticipantResponse>>> getParticipants(@PathVariable String matchId) {
        List<MatchParticipantResponse> response = matchService.getParticipants(matchId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{matchId}/participants")
    public ResponseEntity<ApiResponse<MatchParticipantResponse>> joinMatch(
            @PathVariable String matchId,
            @AuthenticationPrincipal String userId
    ) {
        MatchParticipantResponse response = matchJoinFacade.joinMatchWithDistributedLock(matchId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/{matchId}/participants/me")
    public ResponseEntity<ApiResponse<Void>> leaveMatch(
            @PathVariable String matchId,
            @AuthenticationPrincipal String userId
    ) {
        matchService.leaveMatch(matchId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/{matchId}/confirm")
    public ResponseEntity<ApiResponse<MatchDetailResponse>> confirmMatch(
            @PathVariable String matchId,
            @AuthenticationPrincipal String hostId
    ) {
        MatchDetailResponse response = matchService.confirmMatch(matchId, hostId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<ApiResponse<Void>> cancelMatch(
            @PathVariable String matchId,
            @AuthenticationPrincipal String hostId
    ) {
        matchService.cancelMatch(matchId, hostId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
