package com.back.sportteam.domain.user.entity;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.global.util.TimeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "profile_img", length = 500)
    private String profileImg;

    @Column(length = 20)
    private String position;

    @Column(name = "active_region", length = 100)
    private String activeRegion;

    @Column(name = "preferred_sport", length = 50)
    private String preferredSport;

    @Column(name = "manner_score", nullable = false)
    private Double mannerScore = 0.0;

    @Column(name = "skill_score", nullable = false)
    private Double skillScore = 0.0;

    @Column(name = "manner_rating_sum", precision = 5, scale = 1, nullable = false)
    private BigDecimal mannerRatingSum = BigDecimal.ZERO;

    @Column(name = "manner_review_count", nullable = false)
    private int mannerReviewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private User(String email, String nickname, String passwordHash, UserRole role,
                 AuthProvider provider, String providerId) {
        LocalDateTime now = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static User local(String email, String nickname, String passwordHash, UserRole role) {
        return new User(email, nickname, passwordHash, role, AuthProvider.LOCAL, null);
    }

    public static User google(String email, String nickname, UserRole role, String providerId) {
        return new User(email, nickname, null, role, AuthProvider.GOOGLE, providerId);
    }

    public void updateProfile(
            String nickname,
            String position,
            String activeRegion,
            String preferredSport,
            String profileImg
    ) {
        if (nickname != null) this.nickname = nickname;
        if (position != null) this.position = position;
        if (activeRegion != null) this.activeRegion = activeRegion;
        if (preferredSport != null) this.preferredSport = preferredSport;
        if (profileImg != null) this.profileImg = profileImg;
    }

    public void addMannerRating(BigDecimal rating) {
        this.mannerRatingSum = this.mannerRatingSum.add(rating);
        this.mannerReviewCount++;
        this.mannerScore = this.mannerRatingSum
                .divide(BigDecimal.valueOf(this.mannerReviewCount), 1, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
    }
}