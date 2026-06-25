package com.back.sportteam.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "facility_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilityReview {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "match_id", columnDefinition = "CHAR(36)", nullable = false)
    private String matchId;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(name = "facility_id", columnDefinition = "CHAR(36)", nullable = false)
    private String facilityId;

    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private FacilityReview(String matchId, String userId, String facilityId,
                            BigDecimal rating, String comment) {
        this.id = UUID.randomUUID().toString();
        this.matchId = matchId;
        this.userId = userId;
        this.facilityId = facilityId;
        this.rating = rating;
        this.comment = comment;
    }

    public static FacilityReview create(String matchId, String userId, String facilityId,
                                         BigDecimal rating, String comment) {
        return new FacilityReview(matchId, userId, facilityId, rating, comment);
    }
}
