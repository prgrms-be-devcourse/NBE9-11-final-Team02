package com.back.sportteam.domain.mypage.controller;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.MyMatchStatus;
import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MatchPaymentResponse;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import com.back.sportteam.domain.mypage.dto.response.MyRecordResponse;
import com.back.sportteam.domain.mypage.service.MyPageMatchService;
import com.back.sportteam.domain.mypage.service.MyPagePaymentService;
import com.back.sportteam.domain.mypage.service.MyPageRecordService;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MypageController {

    private final MyPageMatchService mypageMatchService;
    private final MyPagePaymentService mypagePaymentService;
    private final MyPageRecordService myPageRecordService;

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<MyRecordResponse>> getMyRecord(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(myPageRecordService.getMyRecord(userId)));
    }

    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<Page<MyMatchResponse>>> getMyMatches(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) SportType sportType,
            @RequestParam(required = false) MyMatchStatus myMatchStatus,
            @RequestParam(required = false) MatchParticipantRole role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        MyMatchCondition condition = new MyMatchCondition(sportType, myMatchStatus, role, page, size);
        Page<MyMatchResponse> response = mypageMatchService.getMyMatches(userId, condition);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/matches/{matchId}/payment")
    public ResponseEntity<ApiResponse<MatchPaymentResponse>> getMatchPayment(
            @AuthenticationPrincipal String userId,
            @PathVariable String matchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                mypagePaymentService.getMatchPayment(userId, matchId)));
    }
}
