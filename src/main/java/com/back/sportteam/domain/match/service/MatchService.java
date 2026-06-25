package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.dto.request.MatchRecommendationRequest;
import com.back.sportteam.domain.match.dto.request.MatchSearchCondition;
import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.dto.response.MatchDetailResponse;
import com.back.sportteam.domain.match.dto.response.MatchParticipantResponse;
import com.back.sportteam.domain.match.dto.response.MatchRecommendationResponse;
import com.back.sportteam.domain.match.dto.response.MatchSummaryResponse;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchQueryRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.service.PaymentRefundRequestService;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchQueryRepository matchQueryRepository;
    private final FacilitySlotRepository facilitySlotRepository;
    private final PaymentRefundRequestService paymentRefundRequestService;
    private final UserSportStatRepository userSportStatRepository;

    @Value("${match.payment-hold.duration-minutes:1}")
    private long paymentHoldMinutes = 1L;

    @Transactional
    public MatchCreateResponse createMatch(String hostId, MatchCreateRequest request) {
        validateSkillLevelRange(request.minSkillLevel(), request.maxSkillLevel());
        validateDeadlineRange(
                request.recruitDeadline(),
                request.participantCancelDeadline(),
                request.hostCancelDeadline()
        );
        FacilitySlot facilitySlot = getFacilitySlotForUpdate(request.reservationId());
        validateReservationAvailable(request.reservationId());
        facilitySlot.holdUntil(LocalDateTime.now(SERVICE_ZONE).plus(paymentHoldDuration()));

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
                .matchDate(facilitySlot.getSlotDate())
                .startTime(facilitySlot.getStartTime())
                .endTime(facilitySlot.getEndTime())
                .recruitDeadline(request.recruitDeadline())
                .participantCancelDeadline(request.participantCancelDeadline())
                .hostCancelDeadline(request.hostCancelDeadline())
                .build());

        validateSportStatExists(hostId, match);

        Match savedMatch = matchRepository.save(match);
        matchParticipantRepository.save(MatchParticipant.host(savedMatch, hostId));

        return MatchCreateResponse.from(savedMatch);
    }

    @Transactional(readOnly = true)
    public Page<MatchSummaryResponse> getMatches(MatchSearchCondition condition) {
        return matchQueryRepository.findAll(condition)
                .map(MatchSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public List<MatchRecommendationResponse> recommendMatches(String userId, MatchRecommendationRequest request) {
        BigDecimal userSkillScore = userSportStatRepository.findByUser_IdAndSportType(userId, request.sportType())
                .map(UserSportStat::getSkillRating)
                .orElse(BigDecimal.valueOf(SkillLevel.LEVEL_3.getScore()));

        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        int size = request.recommendationSize();
        return matchQueryRepository.findRecommendationCandidates(
                        request.sportType(),
                        MatchStatus.RECRUITING,
                        now,
                        size * 5
                )
                .stream()
                .filter(match -> isSkillLevelMatched(userSkillScore, match))
                .map(match -> recommend(match, userSkillScore, request.gender(), now))
                .sorted(Comparator
                        .comparingInt(MatchRecommendationResponse::recommendationScore)
                        .reversed()
                        .thenComparing(MatchRecommendationResponse::recruitDeadline)
                        .thenComparing(MatchRecommendationResponse::matchId))
                .limit(size)
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

    private MatchParticipantResponse joinMatch(Match match, String matchId, String userId) {
        validateJoinable(match);
        validateNotHost(match, userId);
        validateNotParticipated(matchId, userId);
        validateSportStatExists(userId, match);

        match.increaseCurrentCount();
        MatchParticipant participant = matchParticipantRepository.save(MatchParticipant.participant(match, userId));

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
        matchParticipantRepository.findByMatchIdAndStatusIn(
                        matchId,
                        List.of(MatchParticipantStatus.ACTIVE)
                )
                .forEach(MatchParticipant::cancel);
        releaseFacilitySlot(match.getReservationId());
        paymentRefundRequestService.requestMatchRefunds(
                matchId,
                match.getReservationId(),
                PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST,
                cancelledAt
        );
    }

    private void validateSportStatExists(String userId, Match match) {
        UserSportStat sportStat = userSportStatRepository.findByUser_IdAndSportType(userId, match.getSportType())
                .orElseThrow(() -> new BusinessException(UserErrorCode.SPORT_STAT_NOT_FOUND));

        validateSkillLevelMatched(sportStat.getSkillRating(), match);
    }

    private void validateSkillLevelMatched(BigDecimal skillRating, Match match) {
        if (!isSkillLevelMatched(skillRating, match)) {
            throw new BusinessException(MatchErrorCode.SKILL_LEVEL_NOT_MATCHED);
        }
    }

    private boolean isSkillLevelMatched(BigDecimal skillRating, Match match) {
        if (match.getMinSkillLevel().isAny() && match.getMaxSkillLevel().isAny()) {
            return true;
        }
        BigDecimal minScore = BigDecimal.valueOf(match.getMinSkillLevel().getScore());
        BigDecimal maxScore = BigDecimal.valueOf(match.getMaxSkillLevel().getScore());
        return skillRating.compareTo(minScore) >= 0 && skillRating.compareTo(maxScore) <= 0;
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

    private FacilitySlot getFacilitySlotForUpdate(String reservationId) {
        FacilitySlot facilitySlot = facilitySlotRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND));
        if (!facilitySlot.isReservable()) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_AVAILABLE);
        }
        return facilitySlot;
    }

    private void releaseFacilitySlot(String reservationId) {
        FacilitySlot facilitySlot = facilitySlotRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND));
        facilitySlot.release();
    }

    private void validateDeadlineRange(
            LocalDateTime recruitDeadline,
            LocalDateTime participantCancelDeadline,
            LocalDateTime hostCancelDeadline
    ) {
        if (recruitDeadline.isBefore(participantCancelDeadline)
                || participantCancelDeadline.isBefore(hostCancelDeadline)) {
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
                List.of(MatchParticipantStatus.ACTIVE)
        );
        if (alreadyParticipated) {
            throw new BusinessException(MatchErrorCode.ALREADY_PARTICIPATED);
        }
    }

    private void validateNotHost(Match match, String userId) {
        if (match.isHostedBy(userId)) {
            throw new BusinessException(MatchErrorCode.HOST_CANNOT_JOIN);
        }
    }

    private MatchRecommendationResponse recommend(
            Match match,
            BigDecimal userSkillScore,
            RequiredGender gender,
            LocalDateTime now
    ) {
        RecommendationScore score = new RecommendationScore();
        applySkillScore(match, userSkillScore, score);
        applyGenderScore(match, gender, score);
        applySeatScore(match, score);
        applyDeadlineScore(match, now, score);
        return MatchRecommendationResponse.of(match, score.value, score.reasons);
    }

    private void applySkillScore(Match match, BigDecimal userSkillScore, RecommendationScore score) {
        if (match.getMinSkillLevel().isAny() && match.getMaxSkillLevel().isAny()) {
            score.add(20, "실력 제한이 없는 매칭입니다.");
            return;
        }
        BigDecimal minScore = BigDecimal.valueOf(match.getMinSkillLevel().getScore());
        BigDecimal maxScore = BigDecimal.valueOf(match.getMaxSkillLevel().getScore());
        if (isSkillLevelMatched(userSkillScore, match)) {
            score.add(40, "실력 조건이 일치합니다.");
            return;
        }
        BigDecimal distance = userSkillScore.compareTo(minScore) < 0
                ? minScore.subtract(userSkillScore)
                : userSkillScore.subtract(maxScore);
        int partialScore = Math.max(0, 25 - distance.multiply(BigDecimal.TEN).intValue());
        if (partialScore > 0) {
            score.add(partialScore, "실력 조건과 근접합니다.");
        }
    }

    private void applyGenderScore(Match match, RequiredGender gender, RecommendationScore score) {
        if (match.getRequiredGender() == RequiredGender.ANY || match.getRequiredGender() == RequiredGender.MIXED) {
            score.add(20, "성별 제한이 없는 매칭입니다.");
            return;
        }
        if (gender != null && match.getRequiredGender() == gender) {
            score.add(20, "성별 조건이 일치합니다.");
        }
    }

    private void applySeatScore(Match match, RecommendationScore score) {
        int remainingSeats = match.getCapacity() - match.getCurrentCount();
        if (remainingSeats <= 2) {
            score.add(15, "마감이 임박한 인기 매칭입니다.");
            return;
        }
        BigDecimal fillRate = BigDecimal.valueOf(match.getCurrentCount())
                .divide(BigDecimal.valueOf(match.getCapacity()), 2, RoundingMode.HALF_UP);
        if (fillRate.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            score.add(10, "참가자가 절반 이상 모였습니다.");
        }
    }

    private void applyDeadlineScore(Match match, LocalDateTime now, RecommendationScore score) {
        long hoursUntilDeadline = Duration.between(
                now.atZone(SERVICE_ZONE),
                match.getRecruitDeadline().atZone(SERVICE_ZONE)
        ).toHours();
        if (hoursUntilDeadline <= 24) {
            score.add(15, "모집 마감이 임박했습니다.");
        } else if (hoursUntilDeadline <= 72) {
            score.add(10, "곧 모집이 마감됩니다.");
        }
    }

    private static class RecommendationScore {
        private int value;
        private final List<String> reasons = new java.util.ArrayList<>();

        private void add(int score, String reason) {
            value += score;
            reasons.add(reason);
        }
    }

    private void validateLeaveable(Match match, MatchParticipant participant, LocalDateTime now) {
        if (participant.isHost()) {
            throw new BusinessException(MatchErrorCode.HOST_CANNOT_LEAVE);
        }
        if (match.isParticipantCancelDeadlinePassed(now)) {
            throw new BusinessException(MatchErrorCode.LEAVE_DEADLINE_PASSED);
        }
    }

    private void validateConfirmable(Match match, String hostId) {
        if (!match.isHostedBy(hostId)) {
            throw new BusinessException(MatchErrorCode.NOT_MATCH_OWNER);
        }
        if (!match.isRecruiting()) {
            throw new BusinessException(MatchErrorCode.MATCH_NOT_RECRUITING);
        }
        long activeParticipantCount = matchParticipantRepository.countByMatchIdAndStatus(
                match.getId(),
                MatchParticipantStatus.ACTIVE
        );
        if (activeParticipantCount < match.getCapacity()) {
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
        if (match.isHostCancelDeadlinePassed(LocalDateTime.now(SERVICE_ZONE))) {
            throw new BusinessException(MatchErrorCode.CANCEL_DEADLINE_PASSED);
        }
    }

    private Duration paymentHoldDuration() {
        return Duration.ofMinutes(paymentHoldMinutes);
    }
}
