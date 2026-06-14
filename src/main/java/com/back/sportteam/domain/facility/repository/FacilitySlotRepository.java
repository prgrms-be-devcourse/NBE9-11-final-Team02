package com.back.sportteam.domain.facility.repository;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface FacilitySlotRepository extends JpaRepository<FacilitySlot, String> {

    boolean existsByFacilityIdAndStatusIn(String facilityId, List<SlotStatus> statuses);

    boolean existsByFacilityIdAndSlotDateAndStartTime(String facilityId, LocalDate slotDate, LocalTime startTime);

    java.util.Optional<FacilitySlot> findByIdAndFacilityId(String id, String facilityId);

    List<FacilitySlot> findAllByFacilityIdAndSlotDateOrderByStartTime(String facilityId, LocalDate slotDate);
}
