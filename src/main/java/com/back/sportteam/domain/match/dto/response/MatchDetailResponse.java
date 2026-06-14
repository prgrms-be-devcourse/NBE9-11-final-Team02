package com.back.sportteam.domain.match.dto.response;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;

import java.time.LocalDateTime;

public record MatchDetailResponse(
        String matchId,
        String reservationId,
        String hostId,
        String title,
        SportType sportType,
        int minParticipants,
        int maxParticipants,
        int currentCount,
        int feePerPerson,
        SkillLevel minSkillLevel,
        SkillLevel maxSkillLevel,
        RequiredGender requiredGender,
        LocalDateTime recruitDeadline,
        LocalDateTime cancelDeadline,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        MatchStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MatchDetailResponse from(Match match) {
        return new MatchDetailResponse(
                match.getId(),
                match.getReservationId(),
                match.getHostId(),
                match.getTitle(),
                match.getSportType(),
                match.getMinParticipants(),
                match.getMaxParticipants(),
                match.getCurrentCount(),
                match.getFeePerPerson(),
                match.getMinSkillLevel(),
                match.getMaxSkillLevel(),
                match.getRequiredGender(),
                match.getRecruitDeadline(),
                match.getCancelDeadline(),
                match.getConfirmedAt(),
                match.getCancelledAt(),
                match.getStatus(),
                match.getCreatedAt(),
                match.getUpdatedAt()
        );
    }
}
