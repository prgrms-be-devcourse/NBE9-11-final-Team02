package com.back.sportteam.domain.review.repository;

import com.back.sportteam.domain.review.entity.FacilityReview;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FacilityReviewRepository extends JpaRepository<FacilityReview, String> {

    boolean existsByMatchIdAndUserId(String matchId, String userId);
}
