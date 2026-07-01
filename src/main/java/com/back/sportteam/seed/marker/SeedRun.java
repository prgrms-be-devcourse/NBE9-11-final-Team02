package com.back.sportteam.seed.marker;

import com.back.sportteam.global.util.TimeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시더 실행 마커. (seed_type, seed_version)의 존재 여부로 멱등성을 보장한다.
 * 이미 실행된 시더는 재실행 시 즉시 skip 한다.
 */
@Getter
@Entity
@Table(
        name = "seed_run",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seed_run_type_version",
                columnNames = {"seed_type", "seed_version"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeedRun {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    @Column(name = "seed_type", nullable = false, length = 30)
    private String seedType;

    @Column(name = "seed_version", nullable = false)
    private int seedVersion;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    private SeedRun(String seedType, int seedVersion) {
        this.id = UUID.randomUUID().toString();
        this.seedType = seedType;
        this.seedVersion = seedVersion;
        this.executedAt = LocalDateTime.now(TimeUtils.SERVICE_ZONE);
    }

    public static SeedRun of(String seedType, int seedVersion) {
        return new SeedRun(seedType, seedVersion);
    }
}
