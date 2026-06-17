package com.back.sportteam.domain.user.repository;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.UserSportStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSportStatRepository extends JpaRepository<UserSportStat, String> {

    Optional<UserSportStat> findByUser_IdAndSportType(UUID userId, SportType sportType);
}
