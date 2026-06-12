package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    private static final LocalDateTime CANCEL_DEADLINE = LocalDateTime.of(2099, Month.JANUARY, 1, 10, 0);

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    void 매칭방을_생성하면_방장_참가자도_함께_생성한다() {
        MatchCreateRequest request = createRequest(2, 10);
        when(matchRepository.existsByReservationId(request.reservationId())).thenReturn(false);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchCreateResponse response = matchService.createMatch("host-id", request);

        assertThat(response.matchId()).isNotBlank();
        assertThat(response.reservationId()).isEqualTo("reservation-id");
        assertThat(response.hostId()).isEqualTo("host-id");
        assertThat(response.currentCount()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(MatchStatus.RECRUITING);

        ArgumentCaptor<MatchParticipant> participantCaptor = ArgumentCaptor.forClass(MatchParticipant.class);
        verify(matchParticipantRepository).save(participantCaptor.capture());

        MatchParticipant participant = participantCaptor.getValue();
        assertThat(participant.getUserId()).isEqualTo("host-id");
        assertThat(participant.getRole()).isEqualTo(MatchParticipantRole.HOST);
        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.ACTIVE);
    }

    @Test
    void 이미_선점된_예약이면_매칭방을_생성하지_않는다() {
        MatchCreateRequest request = createRequest(2, 10);
        when(matchRepository.existsByReservationId(request.reservationId())).thenReturn(true);

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.SLOT_ALREADY_RESERVED);

        verify(matchRepository, never()).save(any(Match.class));
        verify(matchParticipantRepository, never()).save(any(MatchParticipant.class));
    }

    @Test
    void 최소_인원이_최대_인원보다_크면_매칭방을_생성하지_않는다() {
        MatchCreateRequest request = createRequest(10, 2);

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.INVALID_PARTICIPANT_RANGE);

        verify(matchRepository, never()).existsByReservationId(any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void 실력_조건을_무관으로_선택하면_상관없음으로_생성한다() {
        MatchCreateRequest request = new MatchCreateRequest(
                "reservation-id",
                "풋살 매칭",
                SportType.FUTSAL,
                2,
                10,
                10000,
                SkillLevel.ANY,
                SkillLevel.ANY,
                RequiredGender.ANY,
                CANCEL_DEADLINE
        );
        when(matchRepository.existsByReservationId(request.reservationId())).thenReturn(false);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchCreateResponse response = matchService.createMatch("host-id", request);

        assertThat(response.minSkillLevel()).isEqualTo(SkillLevel.ANY);
        assertThat(response.maxSkillLevel()).isEqualTo(SkillLevel.ANY);
        assertThat(response.requiredGender()).isEqualTo(RequiredGender.ANY);
    }

    @Test
    void 최소_실력_레벨이_최대_실력_레벨보다_크면_매칭방을_생성하지_않는다() {
        MatchCreateRequest request = createRequest(2, 10, SkillLevel.LEVEL_4, SkillLevel.LEVEL_2);

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.INVALID_SKILL_LEVEL_RANGE);

        verify(matchRepository, never()).existsByReservationId(any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void 실력_레벨_무관은_최소와_최대를_함께_선택해야_한다() {
        MatchCreateRequest request = createRequest(2, 10, SkillLevel.ANY, SkillLevel.LEVEL_3);

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.INVALID_SKILL_LEVEL_RANGE);

        verify(matchRepository, never()).existsByReservationId(any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    private MatchCreateRequest createRequest(int minParticipants, int maxParticipants) {
        return createRequest(minParticipants, maxParticipants, SkillLevel.LEVEL_2, SkillLevel.LEVEL_4);
    }

    private MatchCreateRequest createRequest(
            int minParticipants,
            int maxParticipants,
            SkillLevel minSkillLevel,
            SkillLevel maxSkillLevel
    ) {
        return new MatchCreateRequest(
                "reservation-id",
                "풋살 매칭",
                SportType.FUTSAL,
                minParticipants,
                maxParticipants,
                10000,
                minSkillLevel,
                maxSkillLevel,
                RequiredGender.MIXED,
                CANCEL_DEADLINE
        );
    }
}
