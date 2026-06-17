package com.back.sportteam.domain.facility.repository;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public interface FacilitySlotRepository extends JpaRepository<FacilitySlot, String> {

    boolean existsByFacilityIdAndStatusIn(String facilityId, List<SlotStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from FacilitySlot slot where slot.id = :slotId")
    java.util.Optional<FacilitySlot> findByIdForUpdate(@Param("slotId") String slotId);

    @Query("SELECT s.slotDate FROM FacilitySlot s WHERE s.facilityId = :facilityId " +
           "AND s.slotDate BETWEEN :fromDate AND :toDate AND s.startTime = :startTime")
    Set<LocalDate> findExistingSlotDatesByFacilityIdAndDateBetweenAndStartTime(
            @Param("facilityId") String facilityId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("startTime") LocalTime startTime);

    java.util.Optional<FacilitySlot> findByIdAndFacilityId(String id, String facilityId);

    List<FacilitySlot> findAllByFacilityIdAndSlotDateOrderByStartTime(String facilityId, LocalDate slotDate);

    @Modifying
    @Query("UPDATE FacilitySlot s SET s.price = :price " +
           "WHERE s.facilityId = :facilityId AND s.status IN :statuses " +
           "AND FUNCTION('DAYOFWEEK', s.slotDate) NOT IN (1, 7)")
    void updateWeekdayPrice(@Param("facilityId") String facilityId,
                             @Param("statuses") List<SlotStatus> statuses,
                             @Param("price") int price);

    @Modifying
    @Query("UPDATE FacilitySlot s SET s.price = :price " +
           "WHERE s.facilityId = :facilityId AND s.status IN :statuses " +
           "AND FUNCTION('DAYOFWEEK', s.slotDate) IN (1, 7)")
    void updateWeekendPrice(@Param("facilityId") String facilityId,
                             @Param("statuses") List<SlotStatus> statuses,
                             @Param("price") int price);
}
