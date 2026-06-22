package com.back.sportteam.domain.mypage.service;

import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import com.back.sportteam.domain.mypage.repository.MatchParticipantQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageMatchService {

    private final MatchParticipantQueryRepository matchParticipantQueryRepository;

    @Transactional(readOnly = true)
    public Page<MyMatchResponse> getMyMatches(String userId, MyMatchCondition condition) {
        return matchParticipantQueryRepository.findMyMatches(userId, condition);
    }
}
