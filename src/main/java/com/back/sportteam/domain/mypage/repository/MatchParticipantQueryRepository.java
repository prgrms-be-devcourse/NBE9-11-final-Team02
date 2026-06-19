package com.back.sportteam.domain.mypage.repository;

import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import org.springframework.data.domain.Page;

public interface MatchParticipantQueryRepository {

    Page<MyMatchResponse> findMyMatches(String userId, MyMatchCondition condition);
}
