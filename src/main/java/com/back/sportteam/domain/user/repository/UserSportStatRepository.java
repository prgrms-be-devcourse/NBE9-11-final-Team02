package com.back.sportteam.domain.user.repository;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.UserSportStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSportStatRepository extends JpaRepository<UserSportStat, String> {

    List<UserSportStat> findByUser_Id(String userId);

    Optional<UserSportStat> findByUser_IdAndSportType(String userId, SportType sportType);

    List<UserSportStat> findByUser_IdAndReviewCountGreaterThan(String userId, int minReviewCount);
}