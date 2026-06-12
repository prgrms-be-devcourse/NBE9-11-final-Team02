package com.back.sportteam.domain.match.repository;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, String> {
}
