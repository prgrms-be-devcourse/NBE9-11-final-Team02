package com.back.sportteam.domain.match.dto.response;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;

import java.time.LocalDateTime;

public record MatchParticipantResponse(
        String participantId,
        String userId,
        String nickname,
        MatchParticipantRole role,
        MatchParticipantStatus status,
        LocalDateTime joinedAt
) {

    public static MatchParticipantResponse from(MatchParticipant participant, String nickname) {
        return new MatchParticipantResponse(
                participant.getId(),
                participant.getUserId(),
                nickname,
                participant.getRole(),
                participant.getStatus(),
                participant.getJoinedAt()
        );
    }
}
