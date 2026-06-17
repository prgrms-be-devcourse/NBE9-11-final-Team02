package com.back.sportteam.batch.notification;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.notification.service.NotificationEventPublisher;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchReminderSchedulerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private FacilitySlotRepository facilitySlotRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private MatchReminderScheduler matchReminderScheduler;

    @Test
    void 경기_시작_1시간_전_매칭에_리마인드_이벤트를_발행한다() {
        ReflectionTestUtils.setField(matchReminderScheduler, "reminderBeforeMinutes", 60L);
        ReflectionTestUtils.setField(matchReminderScheduler, "reminderWindowMinutes", 2L);
        LocalDateTime startAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusHours(1).plusSeconds(10);
        Match match = confirmedMatch();
        Reservation reservation = Reservation.pending("slot-id", startAt.minusDays(1));
        FacilitySlot slot = FacilitySlot.create(
                "facility-id",
                startAt.toLocalDate(),
                startAt.toLocalTime(),
                startAt.plusHours(2).toLocalTime(),
                100_000
        );
        when(matchRepository.findByStatus(MatchStatus.CONFIRMED)).thenReturn(List.of(match));
        when(reservationRepository.findById(match.getReservationId())).thenReturn(Optional.of(reservation));
        when(facilitySlotRepository.findById(reservation.getFacilitySlotId())).thenReturn(Optional.of(slot));

        matchReminderScheduler.publishMatchStartReminders();

        verify(notificationEventPublisher).publishMatchReminder(
                org.mockito.ArgumentMatchers.eq(match.getId()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
    }

    @Test
    void 리마인드_시간창에_없는_매칭은_이벤트를_발행하지_않는다() {
        ReflectionTestUtils.setField(matchReminderScheduler, "reminderBeforeMinutes", 60L);
        ReflectionTestUtils.setField(matchReminderScheduler, "reminderWindowMinutes", 1L);
        LocalDateTime startAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusHours(2);
        Match match = confirmedMatch();
        Reservation reservation = Reservation.pending("slot-id", startAt.minusDays(1));
        FacilitySlot slot = FacilitySlot.create(
                "facility-id",
                startAt.toLocalDate(),
                startAt.toLocalTime(),
                startAt.plusHours(2).toLocalTime(),
                100_000
        );
        when(matchRepository.findByStatus(MatchStatus.CONFIRMED)).thenReturn(List.of(match));
        when(reservationRepository.findById(match.getReservationId())).thenReturn(Optional.of(reservation));
        when(facilitySlotRepository.findById(reservation.getFacilitySlotId())).thenReturn(Optional.of(slot));

        matchReminderScheduler.publishMatchStartReminders();

        verify(notificationEventPublisher, never()).publishMatchReminder(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private Match confirmedMatch() {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .minParticipants(2)
                .maxParticipants(10)
                .feePerPerson(10000)
                .minSkillLevel(SkillLevel.LEVEL_2)
                .maxSkillLevel(SkillLevel.LEVEL_4)
                .requiredGender(RequiredGender.MIXED)
                .recruitDeadline(LocalDateTime.of(2026, Month.JUNE, 17, 10, 0))
                .cancelDeadline(LocalDateTime.of(2026, Month.JUNE, 17, 11, 0))
                .build());
        match.confirm(LocalDateTime.of(2026, Month.JUNE, 17, 12, 0));
        return match;
    }
}
