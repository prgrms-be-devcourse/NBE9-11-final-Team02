package com.back.sportteam.domain.review.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
public class ReviewSubmitRequest {

    @Valid
    private FacilityReviewRequest facilityReview;

    @Valid
    private List<ParticipantReviewRequest> participantReviews = Collections.emptyList();
}
