package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.request.MatchSearchCondition;
import com.back.sportteam.domain.match.dto.request.MatchSortType;
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
import com.back.sportteam.domain.payment.service.PaymentRefundRequestService;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    private static final LocalDateTime CLOSED_RECRUIT_DEADLINE = LocalDateTime.of(2026, Month.JUNE, 1, 10, 0);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, Month.JUNE, 11, 10, 0);
    private static final LocalDateTime RECRUIT_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 10, 10, 0);
    private static final LocalDateTime CANCEL_DEADLINE = LocalDateTime.of(2099, Month.JUNE, 12, 10, 0);

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private FacilitySlotRepository facilitySlotRepository;

    @Mock
    private PaymentRefundRequestService paymentRefundRequestService;

    @InjectMocks
    private MatchService matchService;

    @Test
    void 매칭방을_생성하면_방장_참가자도_함께_생성한다() {
        MatchCreateRequest request = createRequest(10);
        FacilitySlot facilitySlot = createFutureSlot();
        when(facilitySlotRepository.findByIdForUpdate(request.reservationId())).thenReturn(Optional.of(facilitySlot));
        when(matchRepository.existsByReservationId(request.reservationId())).thenReturn(false);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchCreateResponse response = matchService.createMatch("host-id", request);

        assertThat(response.matchId()).isNotBlank();
        assertThat(response.reservationId()).isEqualTo("reservation-id");
        assertThat(response.hostId()).isEqualTo("host-id");
        assertThat(response.currentCount()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(MatchStatus.RECRUITING);
        assertThat(facilitySlot.getStatus()).isEqualTo(SlotStatus.PENDING);
        assertThat(facilitySlot.getPendingUntil()).isNotNull();

        ArgumentCaptor<MatchParticipant> participantCaptor = ArgumentCaptor.forClass(MatchParticipant.class);
        verify(matchParticipantRepository).save(participantCaptor.capture());

        MatchParticipant participant = participantCaptor.getValue();
        assertThat(participant.getUserId()).isEqualTo("host-id");
        assertThat(participant.getRole()).isEqualTo(MatchParticipantRole.HOST);
        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.PAYMENT_PENDING);
        assertThat(participant.getPaymentDeadline()).isEqualTo(participant.getJoinedAt().plusMinutes(1));
    }

    @Test
    void 이미_선점된_예약이면_매칭방을_생성하지_않는다() {
        MatchCreateRequest request = createRequest(10);
        when(facilitySlotRepository.findByIdForUpdate(request.reservationId()))
                .thenReturn(Optional.of(createFutureSlot()));
        when(matchRepository.existsByReservationId(request.reservationId())).thenReturn(true);

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.SLOT_ALREADY_RESERVED);

        verify(matchRepository, never()).save(any(Match.class));
        verify(matchParticipantRepository, never()).save(any(MatchParticipant.class));
    }

    @Test
    void 예약_가능한_시설_슬롯이_아니면_매칭방을_생성하지_않는다() {
        MatchCreateRequest request = createRequest(10);
        FacilitySlot facilitySlot = createFutureSlot();
        facilitySlot.holdUntil(CREATED_AT.plusMinutes(1));
        when(facilitySlotRepository.findByIdForUpdate(request.reservationId())).thenReturn(Optional.of(facilitySlot));

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_NOT_AVAILABLE);

        verify(matchRepository, never()).existsByReservationId(any());
        verify(matchRepository, never()).save(any(Match.class));
        verify(matchParticipantRepository, never()).save(any(MatchParticipant.class));
    }

    @Test
    void 실력_조건을_무관으로_선택하면_상관없음으로_생성한다() {
        MatchCreateRequest request = new MatchCreateRequest(
                "reservation-id",
                "풋살 매칭",
                SportType.FUTSAL,
                10,
                10000,
                SkillLevel.ANY,
                SkillLevel.ANY,
                RequiredGender.ANY,
                RECRUIT_DEADLINE,
                CANCEL_DEADLINE
        );
        when(facilitySlotRepository.findByIdForUpdate(request.reservationId()))
                .thenReturn(Optional.of(createFutureSlot()));
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
        MatchCreateRequest request = createRequest(10, SkillLevel.LEVEL_4, SkillLevel.LEVEL_2);

        assertThatThrownBy(() -> matchService.createMatch("host-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.INVALID_SKILL_LEVEL_RANGE);

        verify(matchRepository, never()).existsByReservationId(any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void 실력_레벨_무관은_최소와_최대를_함께_선택해야_한다() {
        MatchCreateRequest request = createRequest(10, SkillLevel.ANY, SkillLevel.LEVEL_3);

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
        MatchSearchCondition condition = new MatchSearchCondition(
                null,
                null,
                null,
                null,
                null,
                MatchSortType.LATEST,
                0,
                20
        );
        when(matchRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createMatch())));

        Page<MatchSummaryResponse> response = matchService.getMatches(condition);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().matchId()).isNotBlank();
        assertThat(response.getContent().getFirst().title()).isEqualTo("풋살 매칭");
        assertThat(response.getContent().getFirst().feePerPerson()).isEqualTo(10000);
        assertThat(response.getContent().getFirst().status()).isEqualTo(MatchStatus.RECRUITING);
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
        participant.activate();
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
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                match.getId(),
                "participant-id",
                List.of(MatchParticipantStatus.PAYMENT_PENDING, MatchParticipantStatus.ACTIVE)
        )).thenReturn(false);
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchParticipantResponse response = matchService.joinMatch(match.getId(), "participant-id");

        assertThat(response.participantId()).isNotBlank();
        assertThat(response.userId()).isEqualTo("participant-id");
        assertThat(response.role()).isEqualTo(MatchParticipantRole.PARTICIPANT);
        assertThat(response.status()).isEqualTo(MatchParticipantStatus.PAYMENT_PENDING);
        assertThat(response.paymentDeadline()).isEqualTo(response.joinedAt().plusMinutes(1));
        assertThat(match.getCurrentCount()).isEqualTo(2);
    }

    @Test
    void 비관적_락으로_매칭방에_참가한다() {
        Match match = createMatch();
        String matchId = match.getId();
        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId,
                "participant-id",
                List.of(MatchParticipantStatus.PAYMENT_PENDING, MatchParticipantStatus.ACTIVE)
        )).thenReturn(false);
        when(matchParticipantRepository.save(any(MatchParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchParticipantResponse response = matchService.joinMatchWithPessimisticLock(matchId, "participant-id");

        assertThat(response.participantId()).isNotBlank();
        assertThat(response.userId()).isEqualTo("participant-id");
        assertThat(response.role()).isEqualTo(MatchParticipantRole.PARTICIPANT);
        assertThat(response.status()).isEqualTo(MatchParticipantStatus.PAYMENT_PENDING);
        assertThat(response.paymentDeadline()).isEqualTo(response.joinedAt().plusMinutes(1));
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
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId,
                "participant-id",
                List.of(MatchParticipantStatus.PAYMENT_PENDING, MatchParticipantStatus.ACTIVE)
        )).thenReturn(true);

        assertThatThrownBy(() -> matchService.joinMatch(matchId, "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.ALREADY_PARTICIPATED);
    }

    @Test
    void 방장은_자신의_매칭방에_참가_신청할_수_없다() {
        Match match = createMatch();
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.joinMatch(matchId, "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.HOST_CANNOT_JOIN);

        verify(matchParticipantRepository, never()).save(any(MatchParticipant.class));
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
        when(facilitySlotRepository.findById(match.getReservationId()))
                .thenReturn(Optional.of(createFutureSlot()));

        matchService.leaveMatch(match.getId(), "participant-id");

        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(match.getCurrentCount()).isEqualTo(1);
        verify(paymentRefundRequestService).requestParticipantRefunds(
                eq(participant.getId()),
                eq(PaymentRefundRequestService.MATCH_PARTICIPANT_LEFT),
                any(LocalDateTime.class)
        );
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

    @Test
    void 매칭방_참가_취소시_이탈_가능_시간이_지났으면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        MatchParticipant participant = MatchParticipant.participant(match, "participant-id");
        participant.activate();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                matchId,
                "participant-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(Optional.of(participant));
        when(facilitySlotRepository.findById(match.getReservationId()))
                .thenReturn(Optional.of(createPastSlot()));

        assertThatThrownBy(() -> matchService.leaveMatch(matchId, "participant-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.LEAVE_DEADLINE_PASSED);

        verify(paymentRefundRequestService, never()).requestParticipantRefunds(any(), any(), any());
    }

    @Test
    void 방장이_매칭방을_확정한다() {
        Match match = createMatch(1);
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.countByMatchIdAndStatus(matchId, MatchParticipantStatus.ACTIVE))
                .thenReturn(1L);

        MatchDetailResponse response = matchService.confirmMatch(matchId, "host-id");

        assertThat(response.status()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(match.getConfirmedAt()).isNotNull();
    }

    @Test
    void 매칭방_확정시_매칭방이_없으면_예외를_던진다() {
        when(matchRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.confirmMatch("missing-id", "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 매칭방_확정시_방장이_아니면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.confirmMatch(matchId, "other-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.NOT_MATCH_OWNER);
    }

    @Test
    void 매칭방_확정시_모집중이_아니면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        match.confirm(CREATED_AT);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.confirmMatch(matchId, "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_RECRUITING);
    }

    @Test
    void 매칭방_확정시_정원이_모이지_않으면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.countByMatchIdAndStatus(matchId, MatchParticipantStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> matchService.confirmMatch(matchId, "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FULL);
    }

    @Test
    void 현재_인원이_정원이어도_결제완료_인원이_부족하면_확정할_수_없다() {
        Match match = createMatch(2);
        String matchId = match.getId();
        match.increaseCurrentCount();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.countByMatchIdAndStatus(matchId, MatchParticipantStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> matchService.confirmMatch(matchId, "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FULL);
    }

    @Test
    void 방장이_매칭방을_취소한다() {
        Match match = createMatch();
        String matchId = match.getId();
        MatchParticipant host = MatchParticipant.host(match, "host-id");
        MatchParticipant participant = MatchParticipant.participant(match, "participant-id");
        MatchParticipant pendingParticipant = MatchParticipant.participant(match, "pending-id");
        FacilitySlot facilitySlot = createFutureSlot();
        facilitySlot.reserve();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatusIn(
                matchId,
                List.of(MatchParticipantStatus.PAYMENT_PENDING, MatchParticipantStatus.ACTIVE)
        )).thenReturn(List.of(host, participant, pendingParticipant));
        when(facilitySlotRepository.findById(match.getReservationId())).thenReturn(Optional.of(facilitySlot));

        matchService.cancelMatch(matchId, "host-id");

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELLED);
        assertThat(match.getCancelledAt()).isNotNull();
        assertThat(host.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(pendingParticipant.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(facilitySlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(facilitySlot.getPendingUntil()).isNull();
        verify(paymentRefundRequestService).requestMatchRefunds(
                matchId,
                match.getReservationId(),
                PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST,
                match.getCancelledAt()
        );
    }

    @Test
    void 매칭방_취소시_매칭방이_없으면_예외를_던진다() {
        when(matchRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.cancelMatch("missing-id", "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 매칭방_취소시_방장이_아니면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, "other-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.NOT_MATCH_OWNER);
    }

    @Test
    void 매칭방_취소시_이미_취소된_상태면_예외를_던진다() {
        Match match = createMatch();
        String matchId = match.getId();
        match.cancel(CREATED_AT);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_CANCELLABLE);
    }

    @Test
    void 매칭방_취소시_취소_가능_시간이_지났으면_예외를_던진다() {
        Match match = createMatch(10, RECRUIT_DEADLINE, CLOSED_RECRUIT_DEADLINE);
        String matchId = match.getId();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.cancelMatch(matchId, "host-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.CANCEL_DEADLINE_PASSED);
    }

    private MatchCreateRequest createRequest(int capacity) {
        return createRequest(capacity, SkillLevel.LEVEL_2, SkillLevel.LEVEL_4);
    }

    private MatchCreateRequest createRequest(
            int capacity,
            SkillLevel minSkillLevel,
            SkillLevel maxSkillLevel
    ) {
        return new MatchCreateRequest(
                "reservation-id",
                "풋살 매칭",
                SportType.FUTSAL,
                capacity,
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

    private Match createMatch(int capacity) {
        return createMatch(capacity, RECRUIT_DEADLINE);
    }

    private Match createMatch(int capacity, LocalDateTime recruitDeadline) {
        return createMatch(capacity, recruitDeadline, CANCEL_DEADLINE);
    }

    private Match createMatch(int capacity, LocalDateTime recruitDeadline, LocalDateTime cancelDeadline) {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .capacity(capacity)
                .feePerPerson(10000)
                .minSkillLevel(SkillLevel.LEVEL_2)
                .maxSkillLevel(SkillLevel.LEVEL_4)
                .requiredGender(RequiredGender.MIXED)
                .recruitDeadline(recruitDeadline)
                .cancelDeadline(cancelDeadline)
                .build());
    }

    private FacilitySlot createFutureSlot() {
        return FacilitySlot.create(
                "facility-id",
                LocalDate.of(2099, Month.JUNE, 12),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                10000
        );
    }

    private FacilitySlot createPastSlot() {
        return FacilitySlot.create(
                "facility-id",
                LocalDate.of(2026, Month.JUNE, 1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                10000
        );
    }
}
