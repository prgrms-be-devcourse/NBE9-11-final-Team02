package com.back.sportteam.domain.match.repository;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, String> {

    List<MatchParticipant> findByMatchIdAndStatus(String matchId, MatchParticipantStatus status);

    Optional<MatchParticipant> findByMatchIdAndUserIdAndStatus(
            String matchId,
            String userId,
            MatchParticipantStatus status
    );
}
