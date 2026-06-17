package com.back.sportteam.domain.review.service;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.review.dto.request.FacilityReviewRequest;
import com.back.sportteam.domain.review.dto.request.ParticipantReviewRequest;
import com.back.sportteam.domain.review.dto.request.ReviewSubmitRequest;
import com.back.sportteam.domain.review.dto.response.FacilityReviewResponse;
import com.back.sportteam.domain.review.entity.FacilityReview;
import com.back.sportteam.domain.review.entity.ParticipantReview;
import com.back.sportteam.domain.review.exception.ReviewErrorCode;
import com.back.sportteam.domain.review.repository.FacilityReviewRepository;
import com.back.sportteam.domain.review.repository.ParticipantReviewRepository;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final FacilityReviewRepository facilityReviewRepository;
    private final ParticipantReviewRepository participantReviewRepository;
    private final UserSportStatRepository userSportStatRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final ReviewValidator reviewValidator;

    @Transactional(readOnly = true)
    public Page<FacilityReviewResponse> getFacilityReviews(String facilityId, Pageable pageable) {
        return facilityReviewRepository.findByFacilityId(facilityId, pageable)
                .map(FacilityReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public List<FacilityReviewResponse> getMyFacilityReviews(String userId) {
        return facilityReviewRepository.findByUserId(userId).stream()
                .map(FacilityReviewResponse::from)
                .toList();
    }

    @Transactional
    public void submitReview(String matchId, String reviewerId, ReviewSubmitRequest request) {
        Match match = reviewValidator.validateAndGetMatch(matchId);
        reviewValidator.validateParticipant(matchId, reviewerId);

        if (request.getFacilityReview() != null) {
            submitFacilityReview(matchId, reviewerId, request.getFacilityReview());
        }

        if (!request.getParticipantReviews().isEmpty()) {
            submitParticipantReviews(matchId, reviewerId, match, request.getParticipantReviews());
        }
    }

    private void submitFacilityReview(String matchId, String reviewerId, FacilityReviewRequest req) {
        reviewValidator.validateFacilityReview(matchId, reviewerId);
        reviewValidator.validateRating(req.getRating());

        String facilityId = matchRepository.findFacilityIdByMatchId(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        facilityReviewRepository.save(FacilityReview.create(matchId, reviewerId, facilityId,
                req.getRating(), req.getComment()));

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_NOT_FOUND));
        facility.addRating(req.getRating());
    }

    private void submitParticipantReviews(String matchId, String reviewerId, Match match,
                                           List<ParticipantReviewRequest> reviews) {
        Set<String> validRevieweeIds = matchParticipantRepository
                .findByMatchIdAndStatus(matchId, MatchParticipantStatus.ACTIVE)
                .stream()
                .map(MatchParticipant::getUserId)
                .collect(Collectors.toSet());

        for (ParticipantReviewRequest pr : reviews) {
            reviewValidator.validateParticipantReview(matchId, reviewerId, pr.getRevieweeId(), validRevieweeIds);
            if (pr.getMannerRating() != null) reviewValidator.validateRating(pr.getMannerRating());
            if (pr.getSkillRating() != null) reviewValidator.validateRating(pr.getSkillRating());

            if (pr.getMannerRating() == null && pr.getSkillRating() == null) {
                continue;
            }

            participantReviewRepository.save(ParticipantReview.create(
                    matchId, reviewerId, pr.getRevieweeId(),
                    pr.getMannerRating(), pr.getSkillRating(), null
            ));

            UUID revieweeId = UUID.fromString(pr.getRevieweeId());
            User reviewee = userRepository.findById(revieweeId)
                    .orElseThrow(() -> new BusinessException(MatchErrorCode.PARTICIPANT_NOT_FOUND));

            if (pr.getMannerRating() != null) {
                reviewee.addMannerRating(pr.getMannerRating());
            }
            if (pr.getSkillRating() != null) {
                // TODO: 매칭 참가 전 해당 종목에 대한 본인 실력 정보 등록 필수화 구현 후 에러코드 재검토
                UserSportStat stat = userSportStatRepository
                        .findByUser_IdAndSportType(revieweeId, match.getSportType())
                        .orElseThrow(() -> new BusinessException(ReviewErrorCode.NOT_A_PARTICIPANT));
                stat.addSkillRating(pr.getSkillRating());
            }
        }
    }
}
