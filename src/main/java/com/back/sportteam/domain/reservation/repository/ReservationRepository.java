package com.back.sportteam.domain.reservation.repository;

import com.back.sportteam.domain.reservation.entity.Reservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, String> {

    Optional<Reservation> findByFacilitySlotId(String facilitySlotId);
}
