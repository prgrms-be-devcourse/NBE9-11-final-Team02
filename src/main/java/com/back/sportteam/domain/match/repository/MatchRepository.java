package com.back.sportteam.domain.match.repository;

import com.back.sportteam.domain.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, String> {

    boolean existsByReservationId(String reservationId);
}
