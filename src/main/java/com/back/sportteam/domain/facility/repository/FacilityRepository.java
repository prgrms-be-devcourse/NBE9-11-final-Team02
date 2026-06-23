package com.back.sportteam.domain.facility.repository;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, String> {

    Optional<Facility> findByIdAndStatusNot(String id, FacilityStatus status);

    List<Facility> findAllByManagerIdAndStatusNot(String managerId, FacilityStatus status);

    Page<Facility> findAllByStatus(FacilityStatus status, Pageable pageable);
}
