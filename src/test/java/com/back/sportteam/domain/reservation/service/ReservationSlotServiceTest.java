package com.back.sportteam.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.entity.ReservationStatus;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationSlotServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private FacilitySlotRepository facilitySlotRepository;

    @InjectMocks
    private ReservationSlotService reservationSlotService;

    @Test
    void confirmReservationConfirmsReservationAndReservesSlot() {
        Reservation reservation = Reservation.pending("slot-id", LocalDateTime.of(2026, Month.JUNE, 16, 10, 0));
        FacilitySlot slot = createSlot();
        when(reservationRepository.findById("reservation-id")).thenReturn(Optional.of(reservation));
        when(facilitySlotRepository.findById("slot-id")).thenReturn(Optional.of(slot));

        reservationSlotService.confirmReservation("reservation-id");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.RESERVED);
    }

    @Test
    void cancelReservationCancelsReservationAndReleasesSlot() {
        LocalDateTime cancelledAt = LocalDateTime.of(2026, Month.JUNE, 16, 12, 0);
        Reservation reservation = Reservation.pending("slot-id", LocalDateTime.of(2026, Month.JUNE, 16, 10, 0));
        FacilitySlot slot = createSlot();
        slot.reserve();
        when(reservationRepository.findById("reservation-id")).thenReturn(Optional.of(reservation));
        when(facilitySlotRepository.findById("slot-id")).thenReturn(Optional.of(slot));

        reservationSlotService.cancelReservation("reservation-id", cancelledAt);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledAt()).isEqualTo(cancelledAt);
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    private FacilitySlot createSlot() {
        return FacilitySlot.create(
                "facility-id",
                LocalDate.of(2026, Month.JUNE, 20),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                100_000
        );
    }
}
