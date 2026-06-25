package com.back.sportteam.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.notification.entity.Notification;
import com.back.sportteam.domain.notification.entity.NotificationType;
import com.back.sportteam.domain.notification.event.MatchNotificationEvent;
import com.back.sportteam.domain.notification.repository.NotificationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, Month.JUNE, 17, 12, 0);
    private static final LocalDate MATCH_DATE = LocalDate.of(2099, Month.JUNE, 10);
    private static final LocalTime MATCH_START_TIME = LocalTime.of(10, 0);
    private static final LocalTime MATCH_END_TIME = LocalTime.of(12, 0);

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void savesConfirmedNotificationForActiveParticipant() {
        Match match = createMatch();
        MatchParticipant participant = MatchParticipant.participant(match, "user-id");
        participant.activate();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatusIn(
                match.getId(),
                List.of(MatchParticipantStatus.ACTIVE)
        )).thenReturn(List.of(participant));
        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(
                "user-id",
                NotificationType.MATCH_CONFIRMED,
                match.getId()
        )).thenReturn(false);
        when(notificationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.handle(new MatchNotificationEvent(
                match.getId(),
                NotificationType.MATCH_CONFIRMED,
                OCCURRED_AT
        ));

        Notification notification = captureSavedNotification();
        assertThat(notification.getUserId()).isEqualTo("user-id");
        assertThat(notification.getType()).isEqualTo(NotificationType.MATCH_CONFIRMED);
        assertThat(notification.getReferenceId()).isEqualTo(match.getId());
        assertThat(notification.getCreatedAt()).isEqualTo(OCCURRED_AT);
        verify(notificationSseService).sendToUser(notification);
    }

    @Test
    void savesCancelledNotificationForCancelledParticipant() {
        Match match = createMatch();
        MatchParticipant participant = MatchParticipant.participant(match, "user-id");
        participant.activate();
        participant.cancel();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatusIn(
                match.getId(),
                List.of(MatchParticipantStatus.ACTIVE, MatchParticipantStatus.CANCELLED)
        )).thenReturn(List.of(participant));
        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(
                "user-id",
                NotificationType.MATCH_CANCELLED,
                match.getId()
        )).thenReturn(false);
        when(notificationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.handle(new MatchNotificationEvent(
                match.getId(),
                NotificationType.MATCH_CANCELLED,
                OCCURRED_AT
        ));

        Notification notification = captureSavedNotification();
        assertThat(notification.getUserId()).isEqualTo("user-id");
        assertThat(notification.getType()).isEqualTo(NotificationType.MATCH_CANCELLED);
        assertThat(notification.getReferenceId()).isEqualTo(match.getId());
        assertThat(notification.getCreatedAt()).isEqualTo(OCCURRED_AT);
        verify(notificationSseService).sendToUser(notification);
    }

    private Notification captureSavedNotification() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        return captor.getValue().getFirst();
    }

    private Match createMatch() {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("notification test match")
                .sportType(SportType.FUTSAL)
                .capacity(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.LEVEL_2)
                .maxSkillLevel(SkillLevel.LEVEL_4)
                .requiredGender(RequiredGender.MIXED)
                .matchDate(MATCH_DATE)
                .startTime(MATCH_START_TIME)
                .endTime(MATCH_END_TIME)
                .recruitDeadline(OCCURRED_AT.plusDays(1))
                .participantCancelDeadline(OCCURRED_AT.plusDays(2))

                .hostCancelDeadline(OCCURRED_AT.plusDays(2))
                .build());
    }
}
