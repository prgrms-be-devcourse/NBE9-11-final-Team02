package com.back.sportteam.domain.review.entity;

import com.back.sportteam.domain.match.entity.SportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 30)
    private SportType sportType;

    @Column(name = "position", nullable = false, length = 20)
    private String position;

    @Column(name = "skill_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal skillRating;

    @Column(name = "skill_rating_sum", nullable = false, precision = 5, scale = 1)
    private BigDecimal skillRatingSum;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "self_reported_level", nullable = false, length = 15)
    private SelfReportedLevel selfReportedLevel;

    private UserSportStat(String userId, SportType sportType, String position,
                           SelfReportedLevel selfReportedLevel) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.sportType = sportType;
        this.position = position;
        this.selfReportedLevel = selfReportedLevel;
        this.skillRatingSum = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.skillRating = selfReportedLevel.getInitialScore().setScale(2, RoundingMode.HALF_UP);
    }

    public static UserSportStat create(String userId, SportType sportType, String position,
                                        SelfReportedLevel selfReportedLevel) {
        return new UserSportStat(userId, sportType, position, selfReportedLevel);
    }
}
