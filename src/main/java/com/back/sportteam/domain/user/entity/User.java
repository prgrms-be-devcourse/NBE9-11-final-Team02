package com.back.sportteam.domain.user.entity;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
    }

    public static User local(String email, String nickname, String passwordHash, UserRole role) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.passwordHash = passwordHash;
        user.role = role;
        user.provider = AuthProvider.LOCAL;
        return user;
    }

    public static User google(String email, String nickname, UserRole role, String providerId) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.role = role;
        user.provider = AuthProvider.GOOGLE;
        user.providerId = providerId;
        return user;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public AuthProvider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getProfileImg() { return profileImg; }
    public String getPosition() { return position; }
    public String getActiveRegion() { return activeRegion; }
    public String getPreferredSport() { return preferredSport; }
    public Double getMannerScore() { return mannerScore; }
    public Double getSkillScore() { return skillScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}