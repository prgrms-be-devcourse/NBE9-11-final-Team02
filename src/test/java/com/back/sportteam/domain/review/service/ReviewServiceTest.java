package com.back.sportteam.domain.review.service;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityDetails;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.match.entity.*;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.review.dto.request.FacilityReviewRequest;
import com.back.sportteam.domain.review.dto.request.ParticipantReviewRequest;
import com.back.sportteam.domain.review.dto.request.ReviewSubmitRequest;
import com.back.sportteam.domain.review.dto.response.FacilityReviewResponse;
import com.back.sportteam.domain.review.entity.*;
import com.back.sportteam.domain.user.entity.SelfReportedLevel;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.review.exception.ReviewErrorCode;
import com.back.sportteam.domain.review.repository.FacilityReviewRepository;
import com.back.sportteam.domain.review.repository.ParticipantReviewRepository;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private FacilityReviewRepository facilityReviewRepository;
    @Mock private ParticipantReviewRepository participantReviewRepository;
    @Mock private UserSportStatRepository userSportStatRepository;
    @Mock private UserRepository userRepository;
    @Mock private FacilityRepository facilityRepository;
    @Mock private ReviewValidator reviewValidator;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void 내가_남긴_시설_리뷰_목록을_조회할_수_있다() {
        FacilityReview review = FacilityReview.create("match-1", "user-1", "facility-1",
                new BigDecimal("4.5"), "좋아요");
        when(facilityReviewRepository.findByUserId("user-1")).thenReturn(List.of(review));

        List<FacilityReviewResponse> result = reviewService.getMyFacilityReviews("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualByComparingTo("4.5");
        assertThat(result.get(0).getComment()).isEqualTo("좋아요");
    }

    @Test
    void 내가_남긴_시설_리뷰가_없으면_빈_목록을_반환한다() {
        when(facilityReviewRepository.findByUserId("user-1")).thenReturn(List.of());

        List<FacilityReviewResponse> result = reviewService.getMyFacilityReviews("user-1");

        assertThat(result).isEmpty();
    }

    @Test
    void 시설_리뷰_목록을_페이징으로_조회할_수_있다() {
        FacilityReview r1 = FacilityReview.create("match-1", "user-1", "facility-1", new BigDecimal("4.0"), "좋아요");
        FacilityReview r2 = FacilityReview.create("match-2", "user-2", "facility-1", new BigDecimal("3.5"), "보통");
        Pageable pageable = PageRequest.of(0, 10);
        when(facilityReviewRepository.findByFacilityId("facility-1", pageable))
                .thenReturn(new PageImpl<>(List.of(r1, r2)));

        Page<FacilityReviewResponse> result = reviewService.getFacilityReviews("facility-1", pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 리뷰가_없는_시설은_빈_페이지를_반환한다() {
        Pageable pageable = PageRequest.of(0, 10);
        when(facilityReviewRepository.findByFacilityId("facility-1", pageable))
                .thenReturn(Page.empty());

        Page<FacilityReviewResponse> result = reviewService.getFacilityReviews("facility-1", pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void 시설_리뷰를_제출하면_저장되고_시설_평점이_갱신된다() {
        Match match = completedMatch();
        Facility facility = facility();
        ReviewSubmitRequest request = submitRequest(facilityReq("4.5", "좋아요"), List.of());

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchRepository.findFacilityIdByMatchId("match-1")).thenReturn(Optional.of("facility-1"));
        when(facilityRepository.findById("facility-1")).thenReturn(Optional.of(facility));
        when(facilityReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview("match-1", "user-1", request);

        verify(facilityReviewRepository).save(any(FacilityReview.class));
        assertThat(facility.getReviewCount()).isEqualTo(1);
        assertThat(facility.getRatingAvg()).isEqualByComparingTo("4.50");
    }

    @Test
    void 서로_다른_사용자가_같은_시설에_리뷰를_남기면_평점이_누적_평균으로_계산된다() {
        Match match = completedMatch();
        Facility facility = facility();
        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchRepository.findFacilityIdByMatchId("match-1")).thenReturn(Optional.of("facility-1"));
        when(facilityRepository.findById("facility-1")).thenReturn(Optional.of(facility));
        when(facilityReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview("match-1", "user-1", submitRequest(facilityReq("4.0", "좋음"), List.of()));
        reviewService.submitReview("match-1", "user-2", submitRequest(facilityReq("3.0", "보통"), List.of()));

        assertThat(facility.getReviewCount()).isEqualTo(2);
        assertThat(facility.getRatingAvg()).isEqualByComparingTo("3.50");
    }

    @Test
    void 시설을_찾을_수_없으면_예외가_발생한다() {
        Match match = completedMatch();
        ReviewSubmitRequest request = submitRequest(facilityReq("4.0", "좋아요"), List.of());

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchRepository.findFacilityIdByMatchId("match-1")).thenReturn(Optional.of("facility-1"));
        when(facilityRepository.findById("facility-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submitReview("match-1", "user-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_NOT_FOUND);
    }

    @Test
    void facilityId를_찾을_수_없으면_예외가_발생한다() {
        Match match = completedMatch();
        ReviewSubmitRequest request = submitRequest(facilityReq("4.0", "좋아요"), List.of());

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchRepository.findFacilityIdByMatchId("match-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.submitReview("match-1", "user-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 참가자_리뷰를_제출하면_매너_평점과_실력_평점이_갱신된다() {
        Match match = completedMatch();
        User reviewee = User.local("test@test.com", "닉네임", "hash", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        UserSportStat stat = UserSportStat.create("2", SportType.FUTSAL, "FW", SelfReportedLevel.INTERMEDIATE);

        MatchParticipant participant = mock(MatchParticipant.class);
        when(participant.getUserId()).thenReturn("2");

        ReviewSubmitRequest request = submitRequest(null, List.of(participantReq("2", "4.0", "3.5")));

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchParticipantRepository.findByMatchIdAndStatus("match-1", MatchParticipantStatus.ACTIVE))
                .thenReturn(List.of(participant));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewee));
        when(userSportStatRepository.findByUserIdAndSportType("2", SportType.FUTSAL))
                .thenReturn(Optional.of(stat));
        when(participantReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview("match-1", "user-1", request);

        verify(participantReviewRepository).save(any(ParticipantReview.class));
        assertThat(stat.getReviewCount()).isEqualTo(1);
    }

    @Test
    void 참가자_리뷰_없이_시설_리뷰만_제출할_수_있다() {
        Match match = completedMatch();
        Facility facility = facility();
        ReviewSubmitRequest request = submitRequest(facilityReq("4.0", "좋아요"), List.of());

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchRepository.findFacilityIdByMatchId("match-1")).thenReturn(Optional.of("facility-1"));
        when(facilityRepository.findById("facility-1")).thenReturn(Optional.of(facility));
        when(facilityReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview("match-1", "user-1", request);

        verify(participantReviewRepository, never()).save(any());
    }

    @Test
    void 시설_리뷰_없이_참가자_리뷰만_제출할_수_있다() {
        Match match = completedMatch();
        User reviewee = User.local("test@test.com", "닉네임", "hash", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        UserSportStat stat = UserSportStat.create("2", SportType.FUTSAL, "FW", SelfReportedLevel.INTERMEDIATE);

        MatchParticipant participant = mock(MatchParticipant.class);
        when(participant.getUserId()).thenReturn("2");

        ReviewSubmitRequest request = submitRequest(null, List.of(participantReq("2", "4.0", "3.5")));

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchParticipantRepository.findByMatchIdAndStatus("match-1", MatchParticipantStatus.ACTIVE))
                .thenReturn(List.of(participant));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewee));
        when(userSportStatRepository.findByUserIdAndSportType("2", SportType.FUTSAL))
                .thenReturn(Optional.of(stat));
        when(participantReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview("match-1", "user-1", request);

        verify(facilityReviewRepository, never()).save(any());
    }

    @Test
    void 매너_평점만_보내면_매너만_갱신되고_실력은_갱신되지_않는다() {
        Match match = completedMatch();
        User reviewee = User.local("test@test.com", "닉네임", "hash", UserRole.USER);
        ReflectionTestUtils.setField(reviewee, "id", 2L);

        MatchParticipant participant = mock(MatchParticipant.class);
        when(participant.getUserId()).thenReturn("2");

        ParticipantReviewRequest pr = new ParticipantReviewRequest();
        ReflectionTestUtils.setField(pr, "revieweeId", "2");
        ReflectionTestUtils.setField(pr, "mannerRating", new BigDecimal("4.0"));
        ReflectionTestUtils.setField(pr, "skillRating", null);
        ReviewSubmitRequest request = submitRequest(null, List.of(pr));

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchParticipantRepository.findByMatchIdAndStatus("match-1", MatchParticipantStatus.ACTIVE))
                .thenReturn(List.of(participant));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewee));
        when(participantReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview("match-1", "user-1", request);

        verify(participantReviewRepository).save(any(ParticipantReview.class));
        verify(userSportStatRepository, never()).findByUserIdAndSportType(any(), any());
    }

    @Test
    void 실력_평점만_보내면_실력만_갱신되고_매너는_갱신되지_않는다() {
        Match match = completedMatch();
        UserSportStat stat = UserSportStat.create("2", SportType.FUTSAL, "FW", SelfReportedLevel.INTERMEDIATE);

        MatchParticipant participant = mock(MatchParticipant.class);
        when(participant.getUserId()).thenReturn("2");

        ParticipantReviewRequest pr = new ParticipantReviewRequest();
        ReflectionTestUtils.setField(pr, "revieweeId", "2");
        ReflectionTestUtils.setField(pr, "mannerRating", null);
        ReflectionTestUtils.setField(pr, "skillRating", new BigDecimal("3.5"));
        ReviewSubmitRequest request = submitRequest(null, List.of(pr));

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchParticipantRepository.findByMatchIdAndStatus("match-1", MatchParticipantStatus.ACTIVE))
                .thenReturn(List.of(participant));
        when(userSportStatRepository.findByUserIdAndSportType("2", SportType.FUTSAL))
                .thenReturn(Optional.of(stat));
        when(participantReviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview("match-1", "user-1", request);

        verify(participantReviewRepository).save(any(ParticipantReview.class));
        verify(userRepository, never()).findById(any());
        assertThat(stat.getReviewCount()).isEqualTo(1);
    }

    @Test
    void 매너와_실력_평점_모두_없으면_해당_참가자에_대한_리뷰가_저장되지_않는다() {
        Match match = completedMatch();

        MatchParticipant participant = mock(MatchParticipant.class);
        when(participant.getUserId()).thenReturn("2");

        ParticipantReviewRequest pr = new ParticipantReviewRequest();
        ReflectionTestUtils.setField(pr, "revieweeId", "2");
        ReflectionTestUtils.setField(pr, "mannerRating", null);
        ReflectionTestUtils.setField(pr, "skillRating", null);
        ReviewSubmitRequest request = submitRequest(null, List.of(pr));

        when(reviewValidator.validateAndGetMatch("match-1")).thenReturn(match);
        when(matchParticipantRepository.findByMatchIdAndStatus("match-1", MatchParticipantStatus.ACTIVE))
                .thenReturn(List.of(participant));

        reviewService.submitReview("match-1", "user-1", request);

        verify(participantReviewRepository, never()).save(any());
    }

    private Match completedMatch() {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-1")
                .hostId("user-1")
                .title("테스트 경기")
                .sportType(SportType.FUTSAL)
                .minParticipants(6)
                .maxParticipants(12)
                .feePerPerson(10000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.LEVEL_5)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(LocalDateTime.now().plusDays(1))
                .cancelDeadline(LocalDateTime.now().plusDays(1))
                .build());
        ReflectionTestUtils.setField(match, "id", "match-1");
        ReflectionTestUtils.setField(match, "status", MatchStatus.COMPLETED);
        return match;
    }

    private Facility facility() {
        return Facility.create(
                "manager-1", "테스트 풋살장", "서울시 강남구",
                FacilityDetails.builder()
                        .phone("02-1234-5678")
                        .description("설명")
                        .capacity(20)
                        .slotDurationMinutes(60)
                        .defaultWeekdayPrice(50000)
                        .defaultWeekendPrice(70000)
                        .sportTypes(Set.of(SportType.FUTSAL))
                        .build()
        );
    }

    private FacilityReviewRequest facilityReq(String rating, String comment) {
        FacilityReviewRequest req = new FacilityReviewRequest();
        ReflectionTestUtils.setField(req, "rating", new BigDecimal(rating));
        ReflectionTestUtils.setField(req, "comment", comment);
        return req;
    }

    private ParticipantReviewRequest participantReq(String revieweeId, String mannerRating, String skillRating) {
        ParticipantReviewRequest req = new ParticipantReviewRequest();
        ReflectionTestUtils.setField(req, "revieweeId", revieweeId);
        ReflectionTestUtils.setField(req, "mannerRating", new BigDecimal(mannerRating));
        ReflectionTestUtils.setField(req, "skillRating", new BigDecimal(skillRating));
        return req;
    }

    private ReviewSubmitRequest submitRequest(FacilityReviewRequest facilityReview,
                                               List<ParticipantReviewRequest> participantReviews) {
        ReviewSubmitRequest req = new ReviewSubmitRequest();
        ReflectionTestUtils.setField(req, "facilityReview", facilityReview);
        ReflectionTestUtils.setField(req, "participantReviews",
                participantReviews != null ? participantReviews : List.of());
        return req;
    }
}
