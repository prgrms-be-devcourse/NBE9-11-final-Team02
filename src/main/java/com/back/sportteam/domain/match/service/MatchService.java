package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.match.dto.request.MatchCreateRequest;
import com.back.sportteam.domain.match.dto.response.MatchCreateResponse;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional
    public MatchCreateResponse createMatch(String hostId, MatchCreateRequest request) {
        validateParticipantRange(request.minParticipants(), request.maxParticipants());
        validateSkillLevelRange(request.minSkillLevel(), request.maxSkillLevel());
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
                .cancelDeadline(request.cancelDeadline())
                .build());

        Match savedMatch = matchRepository.save(match);
        matchParticipantRepository.save(MatchParticipant.host(savedMatch, hostId));

        return MatchCreateResponse.from(savedMatch);
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
}
