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
@Table(name = "participant_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantReview {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "match_id", columnDefinition = "CHAR(36)", nullable = false)
    private String matchId;

    @Column(name = "reviewer_id", columnDefinition = "CHAR(36)", nullable = false)
    private String reviewerId;

    @Column(name = "reviewee_id", columnDefinition = "CHAR(36)", nullable = false)
    private String revieweeId;

    @Column(name = "manner_rating", precision = 3, scale = 1)
    private BigDecimal mannerRating;

    @Column(name = "skill_rating", precision = 3, scale = 1)
    private BigDecimal skillRating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ParticipantReview(String matchId, String reviewerId, String revieweeId,
                               BigDecimal mannerRating, BigDecimal skillRating, String comment) {
        this.id = UUID.randomUUID().toString();
        this.matchId = matchId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.mannerRating = mannerRating;
        this.skillRating = skillRating;
        this.comment = comment;
    }

    public static ParticipantReview create(String matchId, String reviewerId, String revieweeId,
                                            BigDecimal mannerRating, BigDecimal skillRating, String comment) {
        return new ParticipantReview(matchId, reviewerId, revieweeId, mannerRating, skillRating, comment);
    }
}
