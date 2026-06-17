package com.back.sportteam.domain.review.service;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.review.exception.ReviewErrorCode;
import com.back.sportteam.domain.review.repository.FacilityReviewRepository;
import com.back.sportteam.domain.review.repository.ParticipantReviewRepository;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewValidatorTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private FacilityReviewRepository facilityReviewRepository;
    @Mock private ParticipantReviewRepository participantReviewRepository;

    @InjectMocks
    private ReviewValidator reviewValidator;

    @Test
    void 완료된_경기를_조회하면_Match를_반환한다() {
        Match match = completedMatch();
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(match));

        Match result = reviewValidator.validateAndGetMatch("match-1");

        assertThat(result.getId()).isEqualTo("match-1");
    }

    @Test
    void 존재하지_않는_경기면_예외가_발생한다() {
        when(matchRepository.findById("match-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewValidator.validateAndGetMatch("match-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 완료되지_않은_경기면_예외가_발생한다() {
        Match match = recruitingMatch();
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> reviewValidator.validateAndGetMatch("match-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.MATCH_NOT_COMPLETED);
    }

    @Test
    void 경기_참가자면_검증을_통과한다() {
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                "match-1", "user-1", List.of(MatchParticipantStatus.ACTIVE))).thenReturn(true);

        reviewValidator.validateParticipant("match-1", "user-1");
    }

    @Test
    void 경기_참가자가_아니면_예외가_발생한다() {
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                "match-1", "user-1", List.of(MatchParticipantStatus.ACTIVE))).thenReturn(false);

        assertThatThrownBy(() -> reviewValidator.validateParticipant("match-1", "user-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.NOT_A_PARTICIPANT);
    }

    @Test
    void 시설_리뷰를_이미_작성했으면_예외가_발생한다() {
        when(facilityReviewRepository.existsByMatchIdAndUserId("match-1", "user-1")).thenReturn(true);

        assertThatThrownBy(() -> reviewValidator.validateFacilityReview("match-1", "user-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.ALREADY_REVIEWED_FACILITY);
    }

    @Test
    void 시설_리뷰를_처음_작성하면_검증을_통과한다() {
        when(facilityReviewRepository.existsByMatchIdAndUserId("match-1", "user-1")).thenReturn(false);

        reviewValidator.validateFacilityReview("match-1", "user-1");
    }

    @Test
    void 본인에게_리뷰를_남기면_예외가_발생한다() {
        assertThatThrownBy(() -> reviewValidator.validateParticipantReview(
                "match-1", "user-1", "user-1", Set.of("user-1", "user-2")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.CANNOT_REVIEW_SELF);
    }

    @Test
    void 경기에_참가하지_않은_사람에게_리뷰를_남기면_예외가_발생한다() {
        assertThatThrownBy(() -> reviewValidator.validateParticipantReview(
                "match-1", "user-1", "user-99", Set.of("user-2", "user-3")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.REVIEWEE_NOT_PARTICIPANT);
    }

    @Test
    void 같은_참가자에게_이미_리뷰를_남겼으면_예외가_발생한다() {
        when(participantReviewRepository.existsByMatchIdAndReviewerIdAndRevieweeId(
                "match-1", "user-1", "user-2")).thenReturn(true);

        assertThatThrownBy(() -> reviewValidator.validateParticipantReview(
                "match-1", "user-1", "user-2", Set.of("user-2")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.ALREADY_REVIEWED_PARTICIPANT);
    }

    @Test
    void 참가자_리뷰_검증을_정상적으로_통과한다() {
        when(participantReviewRepository.existsByMatchIdAndReviewerIdAndRevieweeId(
                "match-1", "user-1", "user-2")).thenReturn(false);

        reviewValidator.validateParticipantReview("match-1", "user-1", "user-2", Set.of("user-2"));
    }

    @Test
    void 별점이_0_5단위면_검증을_통과한다() {
        reviewValidator.validateRating(new BigDecimal("4.5"));
        reviewValidator.validateRating(new BigDecimal("1.0"));
        reviewValidator.validateRating(new BigDecimal("0.5"));
    }

    @Test
    void 별점이_0_5단위가_아니면_예외가_발생한다() {
        assertThatThrownBy(() -> reviewValidator.validateRating(new BigDecimal("4.3")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.INVALID_RATING);
    }

    private Match completedMatch() {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-1")
                .hostId("user-1")
                .title("테스트 경기")
                .sportType(SportType.FUTSAL)
                .capacity(12)
                .feePerPerson(10000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.LEVEL_5)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(LocalDateTime.of(2099, 12, 31, 0, 0))
                .cancelDeadline(LocalDateTime.of(2099, 12, 31, 0, 0))
                .build());
        ReflectionTestUtils.setField(match, "id", "match-1");
        ReflectionTestUtils.setField(match, "status", MatchStatus.COMPLETED);
        return match;
    }

    private Match recruitingMatch() {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-1")
                .hostId("user-1")
                .title("테스트 경기")
                .sportType(SportType.FUTSAL)
                .capacity(12)
                .feePerPerson(10000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.LEVEL_5)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(LocalDateTime.of(2099, 12, 31, 0, 0))
                .cancelDeadline(LocalDateTime.of(2099, 12, 31, 0, 0))
                .build());
        ReflectionTestUtils.setField(match, "id", "match-1");
        return match;
    }
}
