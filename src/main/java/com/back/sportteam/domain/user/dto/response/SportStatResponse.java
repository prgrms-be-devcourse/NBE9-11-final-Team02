package com.back.sportteam.domain.user.dto.response;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.SelfReportedLevel;
import com.back.sportteam.domain.user.entity.UserSportStat;

public record SportStatResponse(
        SportType sportType,
        boolean registered,
        SelfReportedLevel selfReportedLevel
) {
    public static SportStatResponse registered(UserSportStat stat) {
        return new SportStatResponse(stat.getSportType(), true, stat.getSelfReportedLevel());
    }

    public static SportStatResponse unregistered(SportType sportType) {
        return new SportStatResponse(sportType, false, null);
    }
}
