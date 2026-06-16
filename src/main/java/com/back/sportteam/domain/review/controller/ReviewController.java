package com.back.sportteam.domain.review.controller;

import com.back.sportteam.domain.review.dto.request.ReviewSubmitRequest;
import com.back.sportteam.domain.review.dto.response.FacilityReviewResponse;
import com.back.sportteam.domain.review.service.ReviewService;
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
            @RequestHeader("X-USER-ID") @NotBlank String userId,
            @Valid @RequestBody ReviewSubmitRequest request
    ) {
        reviewService.submitReview(matchId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @GetMapping("/api/v1/users/me/reviews/facilities")
    public ResponseEntity<ApiResponse<List<FacilityReviewResponse>>> getMyFacilityReviews(
            @RequestHeader("X-USER-ID") @NotBlank String userId
    ) {
        List<FacilityReviewResponse> response = reviewService.getMyFacilityReviews(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
