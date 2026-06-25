package com.back.sportteam.domain.facility.entity;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.global.util.TimeUtils;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "facilities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Facility {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "manager_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String managerId;

    @Column(name = "name", nullable = false, updatable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, updatable = false, length = 255)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "default_weekday_price", nullable = false)
    private int defaultWeekdayPrice;

    @Column(name = "default_weekend_price", nullable = false)
    private int defaultWeekendPrice;

    @Column(name = "slot_open_at")
    private LocalDateTime slotOpenAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FacilityStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "facilities_sports", joinColumns = @JoinColumn(name = "facility_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 30)
    private Set<SportType> sportTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "facility_amenities", joinColumns = @JoinColumn(name = "facility_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", nullable = false, length = 30)
    private Set<Amenity> amenities = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "facility_images", joinColumns = @JoinColumn(name = "facility_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", nullable = false, length = 255)
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_sum", precision = 5, scale = 2)
    private BigDecimal ratingSum = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Facility(String managerId, String name, String address, FacilityDetails details) {
        LocalDateTime now = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
        this.id = UUID.randomUUID().toString();
        this.managerId = managerId;
        this.name = name;
        this.address = address;
        this.status = FacilityStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
        applyDetails(details);
    }

    public static Facility create(String managerId, String name, String address, FacilityDetails details) {
        return new Facility(managerId, name, address, details);
    }

    public void update(FacilityDetails details) {
        applyDetails(details);
    }

    private void applyDetails(FacilityDetails details) {
        this.phone = details.phone();
        this.description = details.description();
        this.capacity = details.capacity();
        this.slotDurationMinutes = details.slotDurationMinutes();
        this.defaultWeekdayPrice = details.defaultWeekdayPrice();
        this.defaultWeekendPrice = details.defaultWeekendPrice();
        this.slotOpenAt = details.slotOpenAt();
        this.sportTypes = details.sportTypes() != null ? new HashSet<>(details.sportTypes()) : new HashSet<>();
        this.amenities = details.amenities() != null ? new HashSet<>(details.amenities()) : new HashSet<>();
        this.imageUrls = details.imageUrls() != null ? new ArrayList<>(details.imageUrls()) : new ArrayList<>();
    }

    public void addRating(BigDecimal rating) {
        this.ratingSum = this.ratingSum.add(rating);
        this.reviewCount++;
        this.ratingAvg = this.ratingSum
                .divide(BigDecimal.valueOf(this.reviewCount), 2, RoundingMode.HALF_UP);
    }

    public void removeImage(String imageUrl) {
        this.imageUrls.remove(imageUrl);
    }

    public void close() {
        this.status = FacilityStatus.CLOSED;
    }

    public boolean isOwnedBy(String managerId) {
        return this.managerId.equals(managerId);
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
    }
}
