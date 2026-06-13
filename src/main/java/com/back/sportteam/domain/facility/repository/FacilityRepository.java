package com.back.sportteam.domain.facility.repository;

import com.back.sportteam.domain.facility.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, String> {
}
