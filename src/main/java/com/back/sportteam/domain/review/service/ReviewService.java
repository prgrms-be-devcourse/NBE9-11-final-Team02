package com.back.sportteam.domain.review.service;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.review.dto.request.ParticipantReviewRequest;
import com.back.sportteam.domain.review.dto.request.ReviewSubmitRequest;
import com.back.sportteam.domain.review.entity.FacilityReview;
import com.back.sportteam.domain.review.entity.ParticipantReview;
import com.back.sportteam.domain.review.entity.UserSportStat;
import com.back.sportteam.domain.review.exception.ReviewErrorCode;
import com.back.sportteam.domain.review.repository.FacilityReviewRepository;
import com.back.sportteam.domain.review.repository.ParticipantReviewRepository;
import com.back.sportteam.domain.review.repository.UserSportStatRepository;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
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
    private final ReviewValidator reviewValidator;

    @Transactional
    public void submitReview(String matchId, String reviewerId, ReviewSubmitRequest request) {
        Match match = reviewValidator.validateAndGetMatch(matchId);
        reviewValidator.validateParticipant(matchId, reviewerId);

        if (request.getFacilityReview() != null) {
            reviewValidator.validateFacilityReview(matchId, reviewerId);
            reviewValidator.validateRating(request.getFacilityReview().getRating());

            String facilityId = matchRepository.findFacilityIdByMatchId(matchId)
                    .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

            facilityReviewRepository.save(FacilityReview.create(
                    matchId, reviewerId, facilityId,
                    request.getFacilityReview().getRating(),
                    request.getFacilityReview().getComment()
            ));
        }

        if (!request.getParticipantReviews().isEmpty()) {
            Set<String> validRevieweeIds = matchParticipantRepository
                    .findByMatchIdAndStatus(matchId, MatchParticipantStatus.ACTIVE)
                    .stream()
                    .map(MatchParticipant::getUserId)
                    .collect(Collectors.toSet());

            for (ParticipantReviewRequest pr : request.getParticipantReviews()) {
                reviewValidator.validateParticipantReview(matchId, reviewerId, pr.getRevieweeId(), validRevieweeIds);
                reviewValidator.validateRating(pr.getMannerRating());
                reviewValidator.validateRating(pr.getSkillRating());

                participantReviewRepository.save(ParticipantReview.create(
                        matchId, reviewerId, pr.getRevieweeId(),
                        pr.getMannerRating(), pr.getSkillRating(), null
                ));

                // TODO: User.id가 ERD와 다르게 Long으로 구현됨 — 추후 UUID로 변경 필요
                User reviewee = userRepository.findById(Long.parseLong(pr.getRevieweeId()))
                        .orElseThrow(() -> new BusinessException(MatchErrorCode.PARTICIPANT_NOT_FOUND));
                reviewee.addMannerRating(pr.getMannerRating());

                // TODO: 매칭 참가 전 종목 자기신고 필수화 구현 후 에러코드 재검토
                UserSportStat stat = userSportStatRepository
                        .findByUserIdAndSportType(pr.getRevieweeId(), match.getSportType())
                        .orElseThrow(() -> new BusinessException(ReviewErrorCode.NOT_A_PARTICIPANT));
                stat.addSkillRating(pr.getSkillRating());
            }
        }
    }
}
