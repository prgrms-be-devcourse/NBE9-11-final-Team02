package com.back.sportteam.domain.match.repository;

import com.back.sportteam.domain.match.entity.Match;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, String> {

    boolean existsByReservationId(String reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select match from Match match where match.id = :matchId")
    Optional<Match> findByIdForUpdate(@Param("matchId") String matchId);
}
