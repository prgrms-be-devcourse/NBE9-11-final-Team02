package com.back.sportteam.domain.review.controller;

import com.back.sportteam.domain.review.dto.request.ReviewSubmitRequest;
import com.back.sportteam.domain.review.dto.response.FacilityReviewResponse;
import com.back.sportteam.domain.review.service.ReviewService;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/matches/{matchId}/reviews")
    public ResponseEntity<ApiResponse<Void>> submitReview(
            @PathVariable String matchId,
            @AuthenticationPrincipal @NotBlank String userId,
            @Valid @RequestBody ReviewSubmitRequest request
    ) {
        reviewService.submitReview(matchId, requireUserId(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @GetMapping("/api/v1/facilities/{facilityId}/reviews")
    public ResponseEntity<ApiResponse<Page<FacilityReviewResponse>>> getFacilityReviews(
            @PathVariable String facilityId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FacilityReviewResponse> response = reviewService.getFacilityReviews(facilityId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/users/me/reviews/facilities")
    public ResponseEntity<ApiResponse<List<FacilityReviewResponse>>> getMyFacilityReviews(
            @AuthenticationPrincipal @NotBlank String userId
    ) {
        List<FacilityReviewResponse> response = reviewService.getMyFacilityReviews(requireUserId(userId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private String requireUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        return userId;
    }
}
