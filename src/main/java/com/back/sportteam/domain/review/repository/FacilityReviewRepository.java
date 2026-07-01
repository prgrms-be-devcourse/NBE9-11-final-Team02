package com.back.sportteam.domain.review.repository;

import com.back.sportteam.domain.review.entity.FacilityReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;


public interface FacilityReviewRepository extends JpaRepository<FacilityReview, String> {

    boolean existsByMatchIdAndUserId(String matchId, String userId);

    List<FacilityReview> findByUserId(String userId);

    Page<FacilityReview> findByFacilityId(String facilityId, Pageable pageable);

    @Query("select r.matchId from FacilityReview r where r.userId = :userId and r.matchId in :matchIds")
    Set<String> findReviewedMatchIds(@Param("userId") String userId, @Param("matchIds") List<String> matchIds);
}
