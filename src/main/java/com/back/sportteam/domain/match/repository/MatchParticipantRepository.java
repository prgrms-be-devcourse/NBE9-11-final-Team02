package com.back.sportteam.domain.match.repository;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, String> {

    List<MatchParticipant> findByMatchIdAndStatus(String matchId, MatchParticipantStatus status);

    List<MatchParticipant> findByStatusAndPaymentDeadlineBefore(
            MatchParticipantStatus status,
            LocalDateTime paymentDeadline
    );

    boolean existsByMatchIdAndUserIdAndStatusIn(
            String matchId,
            String userId,
            Collection<MatchParticipantStatus> statuses
    );

    Optional<MatchParticipant> findByMatchIdAndUserIdAndStatus(String matchId, String userId, MatchParticipantStatus status);
}
