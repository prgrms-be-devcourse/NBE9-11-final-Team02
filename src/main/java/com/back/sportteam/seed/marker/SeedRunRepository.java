package com.back.sportteam.seed.marker;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedRunRepository extends JpaRepository<SeedRun, String> {

    boolean existsBySeedTypeAndSeedVersion(String seedType, int seedVersion);
}
