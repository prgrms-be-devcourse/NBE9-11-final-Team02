package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.dto.response.MatchDetailResponse;
import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.dto.response.MatchSummaryResponse;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    private static final LocalDateTime CLOSED_RECRUIT_DEADLINE = LocalDateTime.of(2026, Month.JUNE, 1, 10, 0);
    private static final LocalDateTime RECRUIT_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 10, 10, 0);
    private static final LocalDateTime CANCEL_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 12, 10, 0);

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
                RECRUIT_DEADLINE,
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

    @Test
    void 모집_마감이_취소_마감보다_늦으면_매칭방을_생성하지_않는다() {
        MatchCreateRequest request = new MatchCreateRequest(
                "reservation-id",
                "풋살 매칭",
                SportType.FUTSAL,
                2,
                10,
                10000,
                SkillLevel.LEVEL_2,
                SkillLevel.LEVEL_4,
                RequiredGender.MIXED,
                CANCEL_DEADLINE.plusDays(1),
                CANCEL_DEADLINE
        );

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.INVALID_DEADLINE_RANGE);

        verify(matchRepository, never()).existsByReservationId(any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void 매칭방_목록을_조회한다() {
        when(matchRepository.findAll()).thenReturn(List.of(createMatch()));

        List<MatchSummaryResponse> response = matchService.getMatches();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().matchId()).isNotBlank();
        assertThat(response.getFirst().title()).isEqualTo("풋살 매칭");
        assertThat(response.getFirst().feePerPerson()).isEqualTo(10000);
        assertThat(response.getFirst().status()).isEqualTo(MatchStatus.RECRUITING);
    }

    @Test
    void 매칭방_단건을_조회한다() {
        Match match = createMatch();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        MatchDetailResponse response = matchService.getMatch(match.getId());

        assertThat(response.matchId()).isEqualTo(match.getId());
        assertThat(response.reservationId()).isEqualTo("reservation-id");
        assertThat(response.hostId()).isEqualTo("host-id");
        assertThat(response.title()).isEqualTo("풋살 매칭");
        assertThat(response.status()).isEqualTo(MatchStatus.RECRUITING);
    }

    @Test
    void 매칭방_단건이_없으면_예외를_던진다() {
        when(matchRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.getMatch("missing-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 매칭방_참가자_목록을_조회한다() {
        Match match = createMatch();
        MatchParticipant participant = MatchParticipant.host(match, "host-id");
        when(matchRepository.existsById(match.getId())).thenReturn(true);
        when(matchParticipantRepository.findByMatchIdAndStatus(match.getId(), MatchParticipantStatus.ACTIVE))
                .thenReturn(List.of(participant));

        List<MatchParticipantResponse> response = matchService.getParticipants(match.getId());

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().participantId()).isNotBlank();
        assertThat(response.getFirst().userId()).isEqualTo("host-id");
        assertThat(response.getFirst().role()).isEqualTo(MatchParticipantRole.HOST);
        assertThat(response.getFirst().status()).isEqualTo(MatchParticipantStatus.ACTIVE);
    }

    @Test
    void 매칭방_참가자_목록_조회시_매칭방이_없으면_예외를_던진다() {
        when(matchRepository.existsById("missing-id")).thenReturn(false);

        assertThatThrownBy(() -> matchService.getParticipants("missing-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 매칭방에_참가한다() {
        Match match = createMatch();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(
                match.getId(),
                "participant-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(false);
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchParticipantResponse response = matchService.joinMatch(match.getId(), "participant-id");

        assertThat(response.participantId()).isNotBlank();
        assertThat(response.userId()).isEqualTo("participant-id");
        assertThat(response.role()).isEqualTo(MatchParticipantRole.PARTICIPANT);
        assertThat(response.status()).isEqualTo(MatchParticipantStatus.ACTIVE);
        assertThat(match.getCurrentCount()).isEqualTo(2);
    }

    @Test
    void 매칭방_참가시_매칭방이_없으면_예외를_던진다() {
        when(matchRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.joinMatch("missing-id", "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 매칭방_참가시_정원이_가득차면_예외를_던진다() {
        Match match = createMatch(1);
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.joinMatch(matchId, "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_FULL);
    }

    @Test
    void 매칭방_참가시_모집_마감_시간이_지났으면_예외를_던진다() {
        Match match = createMatch(10, CLOSED_RECRUIT_DEADLINE);
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.joinMatch(matchId, "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.RECRUIT_DEADLINE_PASSED);
    }

    @Test
    void 매칭방_참가시_이미_참가한_유저면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(
                matchId,
                "participant-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(true);

        assertThatThrownBy(() -> matchService.joinMatch(matchId, "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.ALREADY_PARTICIPATED);
    }

    @Test
    void 매칭방_참가를_취소한다() {
        Match match = createMatch();
        MatchParticipant participant = MatchParticipant.participant(match, "participant-id");
        match.increaseCurrentCount();
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                match.getId(),
                "participant-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(Optional.of(participant));

        matchService.leaveMatch(match.getId(), "participant-id");

        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(match.getCurrentCount()).isEqualTo(1);
    }

    @Test
    void 매칭방_참가_취소시_매칭방이_없으면_예외를_던진다() {
        when(matchRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.leaveMatch("missing-id", "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 매칭방_참가_취소시_참가정보가_없으면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                matchId,
                "participant-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.leaveMatch(matchId, "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 매칭방_참가_취소시_방장이면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        MatchParticipant host = MatchParticipant.host(match, "host-id");
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                matchId,
                "host-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.leaveMatch(matchId, "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.HOST_CANNOT_LEAVE);
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
                RECRUIT_DEADLINE,
                CANCEL_DEADLINE
        );
    }

    private Match createMatch() {
        return createMatch(10);
    }

    private Match createMatch(int maxParticipants) {
        return createMatch(maxParticipants, RECRUIT_DEADLINE);
    }

    private Match createMatch(int maxParticipants, LocalDateTime recruitDeadline) {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .minParticipants(Math.min(2, maxParticipants))
                .maxParticipants(maxParticipants)
                .feePerPerson(10000)
                .minSkillLevel(SkillLevel.LEVEL_2)
                .maxSkillLevel(SkillLevel.LEVEL_4)
                .requiredGender(RequiredGender.MIXED)
                .recruitDeadline(recruitDeadline)
                .cancelDeadline(CANCEL_DEADLINE)
                .build());
    }
}
