package com.back.sportteam.domain.mypage.dto.response;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.MyMatchStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record MyMatchResponse(
        String matchId,
        String title,
        SportType sportType,
        MyMatchStatus myMatchStatus,
        MatchParticipantRole role,
        LocalDate matchDate,
        LocalTime startTime,
        LocalTime endTime
) {
    public static MyMatchResponse of(
            String matchId,
            String title,
            SportType sportType,
            MatchStatus matchStatus,
            MatchParticipantStatus participantStatus,
            MatchParticipantRole role,
            LocalDate matchDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return new MyMatchResponse(
                matchId,
                title,
                sportType,
                MyMatchStatus.of(matchStatus, participantStatus),
                role,
                matchDate,
                startTime,
                endTime
        );
    }
}
