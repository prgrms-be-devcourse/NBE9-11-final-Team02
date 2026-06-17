package com.back.sportteam.domain.match.repository;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, String> {

    boolean existsByReservationId(String reservationId);

    Optional<Match> findByReservationId(String reservationId);

    @Query("""
            select match.id
            from Match match
            where match.status = :status
              and match.recruitDeadline <= :deadline
            order by match.recruitDeadline asc
            """)
    List<String> findIdsByStatusAndRecruitDeadlineBefore(
            @Param("status") MatchStatus status,
            @Param("deadline") LocalDateTime deadline,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select match from Match match where match.id = :matchId")
    Optional<Match> findByIdForUpdate(@Param("matchId") String matchId);

    // TODO: Reservation 엔티티 구현 후 JPA 연관관계로 대체
    @Query(value = """
            SELECT fs.facility_id
            FROM matches m
            JOIN reservations r ON m.reservation_id = r.id
            JOIN facility_slots fs ON r.facility_slot_id = fs.id
            WHERE m.id = :matchId
            """, nativeQuery = true)
    Optional<String> findFacilityIdByMatchId(@Param("matchId") String matchId);
}
