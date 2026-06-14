package com.back.sportteam.domain.match.service;

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
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional
    public MatchCreateResponse createMatch(String hostId, MatchCreateRequest request) {
        validateParticipantRange(request.minParticipants(), request.maxParticipants());
        validateSkillLevelRange(request.minSkillLevel(), request.maxSkillLevel());
        validateDeadlineRange(request.recruitDeadline(), request.cancelDeadline());
        validateReservationAvailable(request.reservationId());

        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId(request.reservationId())
                .hostId(hostId)
                .title(request.title())
                .sportType(request.sportType())
                .minParticipants(request.minParticipants())
                .maxParticipants(request.maxParticipants())
                .feePerPerson(request.feePerPerson())
                .minSkillLevel(request.minSkillLevel())
                .maxSkillLevel(request.maxSkillLevel())
                .requiredGender(request.requiredGender())
                .recruitDeadline(request.recruitDeadline())
                .cancelDeadline(request.cancelDeadline())
                .build());

        Match savedMatch = matchRepository.save(match);
        matchParticipantRepository.save(MatchParticipant.host(savedMatch, hostId));

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

        validateJoinable(match);
        validateNotParticipated(matchId, userId);

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

        validateLeaveable(participant);

        participant.cancel();
        match.decreaseCurrentCount();
    }

    private void validateParticipantRange(int minParticipants, int maxParticipants) {
        if (minParticipants > maxParticipants) {
            throw new BusinessException(MatchErrorCode.INVALID_PARTICIPANT_RANGE);
        }
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
        boolean alreadyParticipated = matchParticipantRepository.existsByMatchIdAndUserIdAndStatus(
                matchId,
                userId,
                MatchParticipantStatus.ACTIVE
        );
        if (alreadyParticipated) {
            throw new BusinessException(MatchErrorCode.ALREADY_PARTICIPATED);
        }
    }

    private void validateLeaveable(MatchParticipant participant) {
        if (participant.isHost()) {
            throw new BusinessException(MatchErrorCode.HOST_CANNOT_LEAVE);
        }
    }
}
