package com.back.sportteam.batch.cancel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.service.PaymentRefundRequestService;
import com.back.sportteam.domain.reservation.service.ReservationSlotService;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchDeadlineProcessorTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private PaymentRefundRequestService paymentRefundRequestService;

    @Mock
    private ReservationSlotService reservationSlotService;

    @InjectMocks
    private MatchDeadlineProcessor matchDeadlineProcessor;

    @Test
    void 정원을_충족한_마감_경기는_자동_확정한다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(1, processedAt.minusMinutes(1));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));

        matchDeadlineProcessor.process(match.getId(), processedAt);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(match.getConfirmedAt()).isEqualTo(processedAt);
        verify(reservationSlotService).confirmReservation(match.getReservationId());
        verify(paymentRefundRequestService, never()).requestMatchRefunds(any(), any(), any(), any());
    }

    @Test
    void 정원에_미달한_마감_경기는_취소하고_환불을_대기열에_등록한다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(2, processedAt.minusMinutes(1));
        MatchParticipant participant = mock(MatchParticipant.class);
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatus(
                match.getId(),
                MatchParticipantStatus.ACTIVE
        )).thenReturn(List.of(participant));

        matchDeadlineProcessor.process(match.getId(), processedAt);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELLED);
        assertThat(match.getCancelledAt()).isEqualTo(processedAt);
        verify(participant).cancel();
        verify(reservationSlotService).cancelReservation(match.getReservationId(), processedAt);
        verify(paymentRefundRequestService).requestMatchRefunds(
                match.getId(),
                match.getReservationId(),
                PaymentRefundRequestService.MATCH_MINIMUM_PARTICIPANTS_NOT_MET,
                processedAt
        );
    }

    @Test
    void 이미_처리된_경기는_다시_처리하지_않는다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(1, processedAt.minusMinutes(1));
        match.confirm(processedAt.minusSeconds(1));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));

        matchDeadlineProcessor.process(match.getId(), processedAt);

        verify(matchParticipantRepository, never()).findByMatchIdAndStatus(any(), any());
        verify(paymentRefundRequestService, never()).requestMatchRefunds(any(), any(), any(), any());
    }

    private Match createMatch(int capacity, LocalDateTime recruitDeadline) {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-" + capacity)
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .capacity(capacity)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(recruitDeadline)
                .cancelDeadline(recruitDeadline.plusHours(1))
                .build());
    }

}
