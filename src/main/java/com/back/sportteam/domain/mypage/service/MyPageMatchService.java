package com.back.sportteam.domain.mypage.service;

import com.back.sportteam.domain.mypage.dto.MyMatchStatus;
import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import com.back.sportteam.domain.mypage.repository.MatchParticipantQueryRepository;
import com.back.sportteam.domain.review.repository.FacilityReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MyPageMatchService {

    private final MatchParticipantQueryRepository matchParticipantQueryRepository;
    private final FacilityReviewRepository facilityReviewRepository;

    @Transactional(readOnly = true)
    public Page<MyMatchResponse> getMyMatches(String userId, MyMatchCondition condition) {
        Page<MyMatchResponse> page = matchParticipantQueryRepository.findMyMatches(userId, condition);

        List<String> completedMatchIds = page.getContent().stream()
                .filter(r -> r.myMatchStatus() == MyMatchStatus.COMPLETED)
                .map(MyMatchResponse::matchId)
                .toList();

        if (completedMatchIds.isEmpty()) {
            return page;
        }

        Set<String> reviewedMatchIds = facilityReviewRepository
                .findReviewedMatchIds(userId, completedMatchIds);

        List<MyMatchResponse> updated = page.getContent().stream()
                .map(r -> r.myMatchStatus() == MyMatchStatus.COMPLETED
                        ? r.withReviewed(reviewedMatchIds.contains(r.matchId()))
                        : r)
                .toList();

        return new PageImpl<>(updated, page.getPageable(), page.getTotalElements());
    }
}
