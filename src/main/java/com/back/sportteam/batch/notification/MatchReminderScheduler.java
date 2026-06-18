package com.back.sportteam.batch.notification;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.notification.service.NotificationEventPublisher;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchReminderScheduler {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final Clock clock;
    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final FacilitySlotRepository facilitySlotRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${app.scheduler.notification.reminder-before-minutes:60}")
    private long reminderBeforeMinutes;

    @Value("${app.scheduler.notification.reminder-window-minutes:1}")
    private long reminderWindowMinutes;

    @Scheduled(fixedDelayString = "${app.scheduler.notification.fixed-delay-ms:60000}")
    public void publishMatchStartReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.plusMinutes(reminderBeforeMinutes);
        LocalDateTime windowEnd = windowStart.plusMinutes(reminderWindowMinutes);

        matchRepository.findByStatus(MatchStatus.CONFIRMED)
                .stream()
                .filter(match -> isMatchStartInWindow(match, windowStart, windowEnd))
                .forEach(match -> notificationEventPublisher.publishMatchReminder(match.getId(), now));
    }

    private boolean isMatchStartInWindow(Match match, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return reservationRepository.findById(match.getReservationId())
                .flatMap(reservation -> facilitySlotRepository.findById(reservation.getFacilitySlotId()))
                .map(this::toMatchStartAt)
                .map(matchStartAt -> !matchStartAt.isBefore(windowStart) && matchStartAt.isBefore(windowEnd))
                .orElse(false);
    }

    private LocalDateTime toMatchStartAt(FacilitySlot slot) {
        return LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
    }
}
