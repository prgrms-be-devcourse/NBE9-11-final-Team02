package com.back.sportteam.domain.mypage.repository;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MyPageRecordRepository extends JpaRepository<MatchParticipant, String> {

    int countByUserIdAndStatusAndMatch_Status(
            String userId,
            MatchParticipantStatus participantStatus,
            MatchStatus matchStatus
    );

    int countByUserIdAndStatusAndMatch_StatusAndRole(
            String userId,
            MatchParticipantStatus participantStatus,
            MatchStatus matchStatus,
            MatchParticipantRole role
    );

    @Query("""
            SELECT mp.match.sportType, COUNT(mp)
            FROM MatchParticipant mp
            WHERE mp.userId = :userId
              AND mp.status = :participantStatus
              AND mp.match.status = :matchStatus
            GROUP BY mp.match.sportType
            """)
    List<Object[]> findSportStats(
            @Param("userId") String userId,
            @Param("participantStatus") MatchParticipantStatus participantStatus,
            @Param("matchStatus") MatchStatus matchStatus
    );

    @Query("""
            SELECT YEAR(mp.match.matchDate), MONTH(mp.match.matchDate), COUNT(mp)
            FROM MatchParticipant mp
            WHERE mp.userId = :userId
              AND mp.status = :participantStatus
              AND mp.match.status = :matchStatus
              AND mp.match.matchDate >= :fromDate
            GROUP BY YEAR(mp.match.matchDate), MONTH(mp.match.matchDate)
            ORDER BY YEAR(mp.match.matchDate) ASC, MONTH(mp.match.matchDate) ASC
            """)
    List<Object[]> findMonthlyStats(
            @Param("userId") String userId,
            @Param("participantStatus") MatchParticipantStatus participantStatus,
            @Param("matchStatus") MatchStatus matchStatus,
            @Param("fromDate") LocalDate fromDate
    );
}
