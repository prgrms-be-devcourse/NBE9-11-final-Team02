package com.back.sportteam.domain.settlement.repository;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.settlement.entity.Settlement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, String> {

    @Query("""
            select match.id
            from Match match
            where match.status = :status
              and match.matchDate < :today
              and not exists (
                  select settlement.id
                  from Settlement settlement
                  where settlement.matchId = match.id
              )
            order by match.matchDate asc
            """)
    List<String> findUnsettledCompletedMatchIds(
            @Param("status") MatchStatus status,
            @Param("today") LocalDate today,
            Pageable pageable
    );
}
