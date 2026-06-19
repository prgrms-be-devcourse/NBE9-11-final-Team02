package com.back.sportteam.domain.mypage.dto;

import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;

public enum MyMatchStatus {
    PARTICIPATING,
    COMPLETED,
    CANCELLED;

    public static MyMatchStatus of(MatchStatus matchStatus, MatchParticipantStatus participantStatus) {
        if (participantStatus == MatchParticipantStatus.ACTIVE) {
            if (matchStatus == MatchStatus.RECRUITING || matchStatus == MatchStatus.CONFIRMED) {
                return PARTICIPATING;
            }
            if (matchStatus == MatchStatus.COMPLETED) {
                return COMPLETED;
            }
        }
        return CANCELLED;
    }
}
