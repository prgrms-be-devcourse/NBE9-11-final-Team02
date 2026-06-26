package com.back.sportteam.domain.match.dto.response;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record MatchSummaryResponse(
        String matchId,
        String title,
        String facilityName,
        String facilityAddress,
        LocalDate matchDate,
        LocalTime startTime,
        LocalTime endTime,
        SportType sportType,
        int currentCount,
        int capacity,
        int feePerPerson,
        SkillLevel minSkillLevel,
        SkillLevel maxSkillLevel,
        RequiredGender requiredGender,
        LocalDateTime recruitDeadline,
        MatchStatus status
) {
    public static MatchSummaryResponse from(Match match, String facilityName, String facilityAddress) {
        return new MatchSummaryResponse(
                match.getId(),
                match.getTitle(),
                facilityName,
                facilityAddress,
                match.getMatchDate(),
                match.getStartTime(),
                match.getEndTime(),
                match.getSportType(),
                match.getCurrentCount(),
                match.getCapacity(),
                match.getFeePerPerson(),
                match.getMinSkillLevel(),
                match.getMaxSkillLevel(),
                match.getRequiredGender(),
                match.getRecruitDeadline(),
                match.getStatus()
        );
    }
}
