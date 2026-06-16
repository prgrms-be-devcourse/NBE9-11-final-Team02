package com.back.sportteam.domain.user.repository;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.UserSportStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSportStatRepository extends JpaRepository<UserSportStat, String> {

    Optional<UserSportStat> findByUserIdAndSportType(String userId, SportType sportType);
}
