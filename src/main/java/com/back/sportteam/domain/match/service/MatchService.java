package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.dto.response.MatchDetailResponse;
import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.dto.response.MatchSummaryResponse;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.service.PaymentRefundRequestService;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final FacilitySlotRepository facilitySlotRepository;
    private final PaymentRefundRequestService paymentRefundRequestService;

    @Value("${match.payment-hold.duration-minutes:1}")
    private long paymentHoldMinutes = 1L;

    @Transactional
    public MatchCreateResponse createMatch(String hostId, MatchCreateRequest request) {
        validateSkillLevelRange(request.minSkillLevel(), request.maxSkillLevel());
        validateDeadlineRange(request.recruitDeadline(), request.cancelDeadline());
        validateReservationAvailable(request.reservationId());

        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId(request.reservationId())
                .hostId(hostId)
                .title(request.title())
                .sportType(request.sportType())
                .capacity(request.capacity())
                .feePerPerson(request.feePerPerson())
                .minSkillLevel(request.minSkillLevel())
                .maxSkillLevel(request.maxSkillLevel())
                .requiredGender(request.requiredGender())
                .recruitDeadline(request.recruitDeadline())
                .cancelDeadline(request.cancelDeadline())
                .build());

        Match savedMatch = matchRepository.save(match);
        matchParticipantRepository.save(MatchParticipant.host(savedMatch, hostId, paymentHoldDuration()));

        return MatchCreateResponse.from(savedMatch);
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> getMatches() {
        return matchRepository.findAll()
                .stream()
                .map(MatchSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchDetailResponse getMatch(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        return MatchDetailResponse.from(match);
    }

    @Transactional(readOnly = true)
    public List<MatchParticipantResponse> getParticipants(String matchId) {
        validateMatchExists(matchId);

        return matchParticipantRepository.findByMatchIdAndStatus(matchId, MatchParticipantStatus.ACTIVE)
                .stream()
                .map(MatchParticipantResponse::from)
                .toList();
    }

    @Transactional
    public MatchParticipantResponse joinMatch(String matchId, String userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        return joinMatch(match, matchId, userId);
    }

    @Transactional
    public MatchParticipantResponse joinMatchWithPessimisticLock(String matchId, String userId) {
        // 비교용 경로: Redis 분산락과 같은 임계 구역을 DB row lock으로 보호한다.
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        return joinMatch(match, matchId, userId);
    }

    private MatchParticipantResponse joinMatch(Match match, String matchId, String userId) {
        validateJoinable(match);
        validateNotParticipated(matchId, userId);

        match.increaseCurrentCount();
        MatchParticipant participant = matchParticipantRepository.save(
                MatchParticipant.participant(match, userId, paymentHoldDuration())
        );

        return MatchParticipantResponse.from(participant);
    }

    @Transactional
    public void leaveMatch(String matchId, String userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));
        MatchParticipant participant = matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                        matchId,
                        userId,
                        MatchParticipantStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(MatchErrorCode.PARTICIPANT_NOT_FOUND));

        LocalDateTime leftAt = LocalDateTime.now(SERVICE_ZONE);
        validateLeaveable(match, participant, leftAt);

        participant.cancel();
        match.decreaseCurrentCount();
        paymentRefundRequestService.requestParticipantRefunds(
                participant.getId(),
                PaymentRefundRequestService.MATCH_PARTICIPANT_LEFT,
                leftAt
        );
    }

    @Transactional
    public MatchDetailResponse confirmMatch(String matchId, String hostId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        validateConfirmable(match, hostId);

        match.confirm(LocalDateTime.now(SERVICE_ZONE));

        return MatchDetailResponse.from(match);
    }

    @Transactional
    public void cancelMatch(String matchId, String hostId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        validateCancellable(match, hostId);

        LocalDateTime cancelledAt = LocalDateTime.now(SERVICE_ZONE);
        match.cancel(cancelledAt);
        matchParticipantRepository.findByMatchIdAndStatus(matchId, MatchParticipantStatus.ACTIVE)
                .forEach(MatchParticipant::cancel);
        paymentRefundRequestService.requestMatchRefunds(
                matchId,
                match.getReservationId(),
                PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST,
                cancelledAt
        );
    }

    private void validateSkillLevelRange(SkillLevel minSkillLevel, SkillLevel maxSkillLevel) {
        boolean onlyOneSideAny = minSkillLevel.isAny() != maxSkillLevel.isAny();
        if (onlyOneSideAny || minSkillLevel.isHigherThan(maxSkillLevel)) {
            throw new BusinessException(MatchErrorCode.INVALID_SKILL_LEVEL_RANGE);
        }
    }

    private void validateReservationAvailable(String reservationId) {
        if (matchRepository.existsByReservationId(reservationId)) {
            throw new BusinessException(MatchErrorCode.SLOT_ALREADY_RESERVED);
        }
    }

    private void validateDeadlineRange(LocalDateTime recruitDeadline, LocalDateTime cancelDeadline) {
        if (recruitDeadline.isAfter(cancelDeadline)) {
            throw new BusinessException(MatchErrorCode.INVALID_DEADLINE_RANGE);
        }
    }

    private void validateMatchExists(String matchId) {
        if (!matchRepository.existsById(matchId)) {
            throw new BusinessException(MatchErrorCode.MATCH_NOT_FOUND);
        }
    }

    private void validateJoinable(Match match) {
        if (!match.isRecruiting()) {
            throw new BusinessException(MatchErrorCode.MATCH_NOT_RECRUITING);
        }
        if (match.isRecruitClosed(LocalDateTime.now(SERVICE_ZONE))) {
            throw new BusinessException(MatchErrorCode.RECRUIT_DEADLINE_PASSED);
        }
        if (match.isFull()) {
            throw new BusinessException(MatchErrorCode.MATCH_FULL);
        }
    }

    private void validateNotParticipated(String matchId, String userId) {
        boolean alreadyParticipated = matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId,
                userId,
                List.of(MatchParticipantStatus.PAYMENT_PENDING, MatchParticipantStatus.ACTIVE)
        );
        if (alreadyParticipated) {
            throw new BusinessException(MatchErrorCode.ALREADY_PARTICIPATED);
        }
    }

    private void validateLeaveable(Match match, MatchParticipant participant, LocalDateTime now) {
        if (participant.isHost()) {
            throw new BusinessException(MatchErrorCode.HOST_CANNOT_LEAVE);
        }
        LocalDateTime leaveDeadline = getMatchStartAt(match).minusHours(24);
        if (!now.isBefore(leaveDeadline)) {
            throw new BusinessException(MatchErrorCode.LEAVE_DEADLINE_PASSED);
        }
    }

    private LocalDateTime getMatchStartAt(Match match) {
        FacilitySlot slot = facilitySlotRepository.findById(match.getReservationId())
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND));
        return LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
    }

    private void validateConfirmable(Match match, String hostId) {
        if (!match.isHostedBy(hostId)) {
            throw new BusinessException(MatchErrorCode.NOT_MATCH_OWNER);
        }
        if (!match.isRecruiting()) {
            throw new BusinessException(MatchErrorCode.MATCH_NOT_RECRUITING);
        }
        if (!match.isFull()) {
            throw new BusinessException(MatchErrorCode.MATCH_NOT_FULL);
        }
    }

    private void validateCancellable(Match match, String hostId) {
        if (!match.isHostedBy(hostId)) {
            throw new BusinessException(MatchErrorCode.NOT_MATCH_OWNER);
        }
        if (!match.isCancellable()) {
            throw new BusinessException(MatchErrorCode.MATCH_NOT_CANCELLABLE);
        }
        if (match.isCancelDeadlinePassed(LocalDateTime.now(SERVICE_ZONE))) {
            throw new BusinessException(MatchErrorCode.CANCEL_DEADLINE_PASSED);
        }
    }

    private Duration paymentHoldDuration() {
        return Duration.ofMinutes(paymentHoldMinutes);
    }
}
