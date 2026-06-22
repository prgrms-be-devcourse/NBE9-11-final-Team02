package com.back.sportteam.domain.match.dto.response;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;

import java.time.LocalDateTime;

public record MatchParticipantResponse(
        String participantId,
        String userId,
        MatchParticipantRole role,
        MatchParticipantStatus status,
        LocalDateTime joinedAt
) {

    public static MatchParticipantResponse from(MatchParticipant participant) {
        return new MatchParticipantResponse(
                participant.getId(),
                participant.getUserId(),
                participant.getRole(),
                participant.getStatus(),
                participant.getJoinedAt()
        );
    }
}
