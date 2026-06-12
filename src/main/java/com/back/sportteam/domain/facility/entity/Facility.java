package com.back.sportteam.domain.facility.entity;

import com.back.sportteam.domain.match.entity.SportType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "manager_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String managerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "max_slots", nullable = false)
    private int maxSlots;

    @Column(name = "slot_open_at")
    private LocalDateTime slotOpenAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FacilityStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "facilities_sports", joinColumns = @JoinColumn(name = "facilities"))
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
    @Column(name = "image_url", nullable = false, length = 255)
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Facility(String managerId, String name, String address, String phone,
                     String description, int slotDurationMinutes, int maxSlots,
                     LocalDateTime slotOpenAt, Set<SportType> sportTypes,
                     Set<Amenity> amenities, List<String> imageUrls) {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        this.id = UUID.randomUUID().toString();
        this.managerId = managerId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.description = description;
        this.slotDurationMinutes = slotDurationMinutes;
        this.maxSlots = maxSlots;
        this.slotOpenAt = slotOpenAt;
        this.status = FacilityStatus.ACTIVE;
        this.sportTypes = new HashSet<>(sportTypes);
        this.amenities = amenities != null ? new HashSet<>(amenities) : new HashSet<>();
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Facility create(String managerId, String name, String address, String phone,
                                  String description, int slotDurationMinutes, int maxSlots,
                                  LocalDateTime slotOpenAt, Set<SportType> sportTypes,
                                  Set<Amenity> amenities, List<String> imageUrls) {
        return new Facility(managerId, name, address, phone, description,
                slotDurationMinutes, maxSlots, slotOpenAt, sportTypes, amenities, imageUrls);
    }

    public void update(String name, String address, String phone, String description,
                       Set<Amenity> amenities, List<String> imageUrls) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.description = description;
        this.amenities = amenities != null ? new HashSet<>(amenities) : new HashSet<>();
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
    }

    public void close() {
        this.status = FacilityStatus.CLOSED;
    }

    public boolean isOwnedBy(String managerId) {
        return this.managerId.equals(managerId);
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now(SERVICE_ZONE);
    }
}
