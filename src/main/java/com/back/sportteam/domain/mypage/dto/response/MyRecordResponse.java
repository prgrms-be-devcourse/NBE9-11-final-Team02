package com.back.sportteam.domain.mypage.dto.response;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserSportStat;

import java.math.BigDecimal;
import java.util.List;

public record MyRecordResponse(
        int totalMatchCount,
        int hostedMatchCount,
        int participatedMatchCount,
        List<SportStat> sportStats,
        List<MonthlyStat> monthlyStats,
        MannerStat mannerStat,
        List<SkillStat> skillStats
) {

    public record SportStat(SportType sportType, long count) {}

    public record MonthlyStat(int year, int month, long count) {}

    public record MannerStat(BigDecimal mannerScore, int mannerReviewCount) {
        public static MannerStat from(User user) {
            return new MannerStat(user.getMannerScore(), user.getMannerReviewCount());
        }
    }

    public record SkillStat(SportType sportType, String position, BigDecimal skillRating, int reviewCount) {
        public static SkillStat from(UserSportStat stat) {
            return new SkillStat(stat.getSportType(), stat.getPosition(), stat.getSkillRating(), stat.getReviewCount());
        }
    }

    public static MyRecordResponse of(
            int totalMatchCount,
            int hostedMatchCount,
            int participatedMatchCount,
            List<SportStat> sportStats,
            List<MonthlyStat> monthlyStats,
            User user,
            List<SkillStat> skillStats
    ) {
        return new MyRecordResponse(
                totalMatchCount,
                hostedMatchCount,
                participatedMatchCount,
                sportStats,
                monthlyStats,
                MannerStat.from(user),
                skillStats
        );
    }
}
