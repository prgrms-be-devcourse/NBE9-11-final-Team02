package com.back.sportteam.domain.review.repository;

import com.back.sportteam.domain.review.entity.FacilityReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface FacilityReviewRepository extends JpaRepository<FacilityReview, String> {

    boolean existsByMatchIdAndUserId(String matchId, String userId);

    List<FacilityReview> findByUserId(String userId);

    Page<FacilityReview> findByFacilityId(String facilityId, Pageable pageable);
}
