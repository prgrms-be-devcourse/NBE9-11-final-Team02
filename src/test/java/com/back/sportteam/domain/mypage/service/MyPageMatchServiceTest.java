package com.back.sportteam.domain.mypage.service;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.MyMatchStatus;
import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import com.back.sportteam.domain.mypage.repository.MatchParticipantQueryRepository;
import com.back.sportteam.domain.review.repository.FacilityReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPageMatchServiceTest {

    @Mock
    private MatchParticipantQueryRepository matchParticipantQueryRepository;

    @Mock
    private FacilityReviewRepository facilityReviewRepository;

    @InjectMocks
    private MyPageMatchService myPageMatchService;

    private static final MyMatchCondition CONDITION = new MyMatchCondition(null, null, null, 0, 10);
    private static final LocalDate DATE = LocalDate.of(2026, Month.JUNE, 1);
    private static final LocalTime START = LocalTime.of(10, 0);
    private static final LocalTime END = LocalTime.of(12, 0);

    @Test
    void COMPLETED_매치에_리뷰를_작성했으면_reviewed가_true다() {
        MyMatchResponse match = response("match-1", MyMatchStatus.COMPLETED);
        Page<MyMatchResponse> page = new PageImpl<>(List.of(match), PageRequest.of(0, 10), 1);

        when(matchParticipantQueryRepository.findMyMatches("user-1", CONDITION)).thenReturn(page);
        when(facilityReviewRepository.findReviewedMatchIds("user-1", List.of("match-1")))
                .thenReturn(Set.of("match-1"));

        Page<MyMatchResponse> result = myPageMatchService.getMyMatches("user-1", CONDITION);

        assertThat(result.getContent().get(0).reviewed()).isTrue();
    }

    @Test
    void COMPLETED_매치에_리뷰를_작성하지_않았으면_reviewed가_false다() {
        MyMatchResponse match = response("match-1", MyMatchStatus.COMPLETED);
        Page<MyMatchResponse> page = new PageImpl<>(List.of(match), PageRequest.of(0, 10), 1);

        when(matchParticipantQueryRepository.findMyMatches("user-1", CONDITION)).thenReturn(page);
        when(facilityReviewRepository.findReviewedMatchIds("user-1", List.of("match-1")))
                .thenReturn(Set.of());

        Page<MyMatchResponse> result = myPageMatchService.getMyMatches("user-1", CONDITION);

        assertThat(result.getContent().get(0).reviewed()).isFalse();
    }

    @Test
    void COMPLETED가_없으면_리뷰_조회를_하지_않는다() {
        MyMatchResponse match = response("match-1", MyMatchStatus.PARTICIPATING);
        Page<MyMatchResponse> page = new PageImpl<>(List.of(match), PageRequest.of(0, 10), 1);

        when(matchParticipantQueryRepository.findMyMatches("user-1", CONDITION)).thenReturn(page);

        Page<MyMatchResponse> result = myPageMatchService.getMyMatches("user-1", CONDITION);

        verifyNoInteractions(facilityReviewRepository);
        assertThat(result.getContent().get(0).reviewed()).isFalse();
    }

    @Test
    void COMPLETED와_비COMPLETED_매치가_섞여_있으면_COMPLETED만_reviewed를_반영한다() {
        MyMatchResponse completed = response("match-1", MyMatchStatus.COMPLETED);
        MyMatchResponse participating = response("match-2", MyMatchStatus.PARTICIPATING);
        Page<MyMatchResponse> page = new PageImpl<>(List.of(completed, participating), PageRequest.of(0, 10), 2);

        when(matchParticipantQueryRepository.findMyMatches("user-1", CONDITION)).thenReturn(page);
        when(facilityReviewRepository.findReviewedMatchIds("user-1", List.of("match-1")))
                .thenReturn(Set.of("match-1"));

        Page<MyMatchResponse> result = myPageMatchService.getMyMatches("user-1", CONDITION);

        assertThat(result.getContent().get(0).reviewed()).isTrue();
        assertThat(result.getContent().get(1).reviewed()).isFalse();
        verify(facilityReviewRepository).findReviewedMatchIds("user-1", List.of("match-1"));
    }

    private MyMatchResponse response(String matchId, MyMatchStatus status) {
        return new MyMatchResponse(matchId, "풋살 매칭", SportType.FUTSAL, status,
                MatchParticipantRole.PARTICIPANT, DATE, START, END, false);
    }
}
