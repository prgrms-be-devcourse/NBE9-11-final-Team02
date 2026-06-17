package com.back.sportteam.domain.review.service;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.review.exception.ReviewErrorCode;
import com.back.sportteam.domain.review.repository.FacilityReviewRepository;
import com.back.sportteam.domain.review.repository.ParticipantReviewRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReviewValidator {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final FacilityReviewRepository facilityReviewRepository;
    private final ParticipantReviewRepository participantReviewRepository;

    public Match validateAndGetMatch(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new BusinessException(ReviewErrorCode.MATCH_NOT_COMPLETED);
        }

        return match;
    }

    public void validateParticipant(String matchId, String userId) {
        if (!matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId, userId, List.of(MatchParticipantStatus.ACTIVE))) {
            throw new BusinessException(ReviewErrorCode.NOT_A_PARTICIPANT);
        }
    }

    public void validateFacilityReview(String matchId, String userId) {
        if (facilityReviewRepository.existsByMatchIdAndUserId(matchId, userId)) {
            throw new BusinessException(ReviewErrorCode.ALREADY_REVIEWED_FACILITY);
        }
    }

    public void validateParticipantReview(String matchId, String reviewerId,
                                           String revieweeId, Set<String> validRevieweeIds) {
        if (revieweeId.equals(reviewerId)) {
            throw new BusinessException(ReviewErrorCode.CANNOT_REVIEW_SELF);
        }
        if (!validRevieweeIds.contains(revieweeId)) {
            throw new BusinessException(ReviewErrorCode.REVIEWEE_NOT_PARTICIPANT);
        }
        if (participantReviewRepository.existsByMatchIdAndReviewerIdAndRevieweeId(
                matchId, reviewerId, revieweeId)) {
            throw new BusinessException(ReviewErrorCode.ALREADY_REVIEWED_PARTICIPANT);
        }
    }

    public void validateRating(BigDecimal rating) {
        BigDecimal remainder = rating.remainder(new BigDecimal("0.5"));
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ReviewErrorCode.INVALID_RATING);
        }
    }
}
