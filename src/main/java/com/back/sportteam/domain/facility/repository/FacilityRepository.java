package com.back.sportteam.domain.facility.repository;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, String> {

    Optional<Facility> findByIdAndStatusNot(String id, FacilityStatus status);
}
