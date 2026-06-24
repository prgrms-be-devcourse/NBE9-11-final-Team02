package com.back.sportteam.domain.user.dto.response;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.UserSportStat;

import java.math.BigDecimal;
import java.util.List;

public record SportStatRegisterResponse(List<SportStatItem> stats) {

    public record SportStatItem(SportType sportType, BigDecimal skillRating) {
        public static SportStatItem from(UserSportStat stat) {
            return new SportStatItem(stat.getSportType(), stat.getSkillRating());
        }
    }

    public static SportStatRegisterResponse from(List<UserSportStat> stats) {
        return new SportStatRegisterResponse(
                stats.stream().map(SportStatItem::from).toList()
        );
    }
}
