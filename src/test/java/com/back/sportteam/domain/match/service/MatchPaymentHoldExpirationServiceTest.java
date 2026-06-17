package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchPaymentHoldExpirationServiceTest {

    private final MatchParticipantRepository matchParticipantRepository = mock(MatchParticipantRepository.class);
    private final MatchPaymentHoldExpirationService service =
            new MatchPaymentHoldExpirationService(matchParticipantRepository);

    @Test
    void 결제_대기_시간이_만료된_참가자를_취소하고_선점_인원을_감소시킨다() {
        Match match = createMatch();
        MatchParticipant participant = MatchParticipant.participant(match, "participant-id");
        match.increaseCurrentCount();
        when(matchParticipantRepository.findByStatusAndPaymentDeadlineBefore(
                any(MatchParticipantStatus.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(participant));

        service.cancelExpiredPaymentHolds();

        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(participant.getPaymentDeadline()).isNull();
        assertThat(match.getCurrentCount()).isEqualTo(1);
        verify(matchParticipantRepository).findByStatusAndPaymentDeadlineBefore(
                any(MatchParticipantStatus.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void 방장_결제_대기_시간이_만료되면_매칭방도_취소한다() {
        Match match = createMatch();
        MatchParticipant host = MatchParticipant.host(match, "host-id");
        when(matchParticipantRepository.findByStatusAndPaymentDeadlineBefore(
                any(MatchParticipantStatus.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(host));

        service.cancelExpiredPaymentHolds();

        assertThat(host.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(match.getCurrentCount()).isZero();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELLED);
        assertThat(match.getCancelledAt()).isNotNull();
    }

    private Match createMatch() {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .maxParticipants(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.LEVEL_1)
                .maxSkillLevel(SkillLevel.LEVEL_5)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(LocalDateTime.of(2099, Month.JUNE, 10, 10, 0))
                .cancelDeadline(LocalDateTime.of(2099, Month.JUNE, 12, 10, 0))
                .build());
    }
}
