package com.back.sportteam.domain.review.dto.response;

import com.back.sportteam.domain.review.entity.FacilityReview;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class FacilityReviewResponse {

    private final String reviewId;
    private final String matchId;
    private final String facilityId;
    private final BigDecimal rating;
    private final String comment;
    private final LocalDateTime createdAt;

    private FacilityReviewResponse(FacilityReview review) {
        this.reviewId = review.getId();
        this.matchId = review.getMatchId();
        this.facilityId = review.getFacilityId();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.createdAt = review.getCreatedAt();
    }

    public static FacilityReviewResponse from(FacilityReview review) {
        return new FacilityReviewResponse(review);
    }
}
