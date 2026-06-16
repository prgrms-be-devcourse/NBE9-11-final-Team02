package com.back.sportteam.domain.review.repository;

import com.back.sportteam.domain.review.entity.ParticipantReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantReviewRepository extends JpaRepository<ParticipantReview, String> {

    boolean existsByMatchIdAndReviewerIdAndRevieweeId(String matchId, String reviewerId, String revieweeId);
}
