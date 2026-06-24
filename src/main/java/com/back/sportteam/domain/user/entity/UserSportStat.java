package com.back.sportteam.domain.user.entity;

import com.back.sportteam.domain.match.entity.SportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_sport_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSportStat {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 30)
    private SportType sportType;

    @Column(name = "position", nullable = false, length = 20)
    private String position;

    @Column(name = "skill_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal skillRating;

    @Column(name = "skill_rating_sum", nullable = false, precision = 5, scale = 2)
    private BigDecimal skillRatingSum;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "self_reported_level", nullable = false, length = 15)
    private SelfReportedLevel selfReportedLevel;

    private UserSportStat(User user, SportType sportType, String position,
                           SelfReportedLevel selfReportedLevel) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.sportType = sportType;
        this.position = position;
        this.selfReportedLevel = selfReportedLevel;
        this.skillRatingSum = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.skillRating = selfReportedLevel.getInitialScore().setScale(2, RoundingMode.HALF_UP);
    }

    public static UserSportStat create(User user, SportType sportType, String position,
                                        SelfReportedLevel selfReportedLevel) {
        return new UserSportStat(user, sportType, position, selfReportedLevel);
    }

    public void addSkillRating(BigDecimal newRating) {
        this.skillRatingSum = this.skillRatingSum.add(newRating);
        this.reviewCount++;
        this.skillRating = calculateEffectiveRating();
    }

    // 콜드 스타트: 리뷰 10개 미만이면 사용자가 등록한 실력값 가중치를 점진적으로 줄이며 반영
    private BigDecimal calculateEffectiveRating() {
        BigDecimal reviewAvg = skillRatingSum.divide(
                BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);

        if (reviewCount >= 10) {
            return reviewAvg;
        }

        double decayCoeff = 1.0 - reviewCount / 10.0;
        double effective = selfReportedLevel.getInitialScore().doubleValue() * decayCoeff
                + reviewAvg.doubleValue() * (1.0 - decayCoeff);

        return BigDecimal.valueOf(effective).setScale(2, RoundingMode.HALF_UP);
    }
}
