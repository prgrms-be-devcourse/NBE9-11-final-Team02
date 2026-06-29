package com.back.sportteam.domain.match.dto.response;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MatchRecommendationResponse(
        String matchId,
        String title,
        String hostNickname,
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
        MatchStatus status,
        int recommendationScore,
        List<String> reasons
) {

    public static MatchRecommendationResponse of(
            Match match,
            String hostNickname,
            String facilityName,
            String facilityAddress,
            int recommendationScore,
            List<String> reasons
    ) {
        return new MatchRecommendationResponse(
                match.getId(),
                match.getTitle(),
                hostNickname,
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
                match.getStatus(),
                recommendationScore,
                reasons
        );
    }
}
