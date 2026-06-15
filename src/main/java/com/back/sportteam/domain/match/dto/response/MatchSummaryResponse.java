package com.back.sportteam.domain.match.dto.response;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;

import java.time.LocalDateTime;

public record MatchSummaryResponse(
        String matchId,
        String title,
        SportType sportType,
        int currentCount,
        int maxParticipants,
        int feePerPerson,
        SkillLevel minSkillLevel,
        SkillLevel maxSkillLevel,
        RequiredGender requiredGender,
        LocalDateTime recruitDeadline,
        MatchStatus status
) {
    public static MatchSummaryResponse from(Match match) {
        return new MatchSummaryResponse(
                match.getId(),
                match.getTitle(),
                match.getSportType(),
                match.getCurrentCount(),
                match.getMaxParticipants(),
                match.getFeePerPerson(),
                match.getMinSkillLevel(),
                match.getMaxSkillLevel(),
                match.getRequiredGender(),
                match.getRecruitDeadline(),
                match.getStatus()
        );
    }
}
