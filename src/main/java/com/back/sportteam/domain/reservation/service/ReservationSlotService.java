package com.back.sportteam.domain.reservation.service;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationSlotService {

    private final ReservationRepository reservationRepository;
    private final FacilitySlotRepository facilitySlotRepository;

    @Transactional
    public void confirmReservation(String reservationId) {
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            reservation.confirm();
            findFacilitySlot(reservation).reserve();
        });
    }

    @Transactional
    public void cancelReservation(String reservationId, LocalDateTime cancelledAt) {
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            reservation.cancel(cancelledAt);
            findFacilitySlot(reservation).release();
        });
    }

    private FacilitySlot findFacilitySlot(Reservation reservation) {
        return facilitySlotRepository.findById(reservation.getFacilitySlotId())
                .orElseThrow(() -> new IllegalStateException("Facility slot not found."));
    }
}
