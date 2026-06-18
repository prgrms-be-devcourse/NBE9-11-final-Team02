package com.back.sportteam.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.LocalDateTime;
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
    void 매칭_확정_이벤트를_참가자_알림으로_저장한다() {
        Match match = createMatch();
        MatchParticipant participant = MatchParticipant.participant(match, "user-id");
        participant.activate();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatus(match.getId(), MatchParticipantStatus.ACTIVE))
                .thenReturn(List.of(participant));
        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(
                "user-id",
                NotificationType.MATCH_CONFIRMED,
                match.getId()
        )).thenReturn(false);
        when(notificationRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.handle(new MatchNotificationEvent(
                match.getId(),
                NotificationType.MATCH_CONFIRMED,
                OCCURRED_AT
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        Notification notification = captor.getValue().getFirst();
        assertThat(notification.getUserId()).isEqualTo("user-id");
        assertThat(notification.getType()).isEqualTo(NotificationType.MATCH_CONFIRMED);
        assertThat(notification.getReferenceId()).isEqualTo(match.getId());
        assertThat(notification.getCreatedAt()).isEqualTo(OCCURRED_AT);
        verify(notificationSseService).sendToUser(notification);
    }

    private Match createMatch() {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .capacity(10)
                .feePerPerson(10000)
                .minSkillLevel(SkillLevel.LEVEL_2)
                .maxSkillLevel(SkillLevel.LEVEL_4)
                .requiredGender(RequiredGender.MIXED)
                .recruitDeadline(OCCURRED_AT.plusDays(1))
                .cancelDeadline(OCCURRED_AT.plusDays(2))
                .build());
    }
}
