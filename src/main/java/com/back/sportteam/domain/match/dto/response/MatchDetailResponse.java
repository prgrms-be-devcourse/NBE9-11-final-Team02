package com.back.sportteam.domain.match.dto.response;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record MatchDetailResponse(
        String matchId,
        String reservationId,
        String hostId,
        String title,
        String facilityName,
        String facilityAddress,
        LocalDate matchDate,
        LocalTime startTime,
        LocalTime endTime,
        SportType sportType,
        int capacity,
        int currentCount,
        int feePerPerson,
        SkillLevel minSkillLevel,
        SkillLevel maxSkillLevel,
        RequiredGender requiredGender,
        LocalDateTime recruitDeadline,
        LocalDateTime participantCancelDeadline,
        LocalDateTime hostCancelDeadline,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        MatchStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MatchDetailResponse from(Match match, String facilityName, String facilityAddress) {
        return new MatchDetailResponse(
                match.getId(),
                match.getReservationId(),
                match.getHostId(),
                match.getTitle(),
                facilityName,
                facilityAddress,
                match.getMatchDate(),
                match.getStartTime(),
                match.getEndTime(),
                match.getSportType(),
                match.getCapacity(),
                match.getCurrentCount(),
                match.getFeePerPerson(),
                match.getMinSkillLevel(),
                match.getMaxSkillLevel(),
                match.getRequiredGender(),
                match.getRecruitDeadline(),
                match.getParticipantCancelDeadline(),
                match.getHostCancelDeadline(),
                match.getConfirmedAt(),
                match.getCancelledAt(),
                match.getStatus(),
                match.getCreatedAt(),
                match.getUpdatedAt()
        );
    }
}
