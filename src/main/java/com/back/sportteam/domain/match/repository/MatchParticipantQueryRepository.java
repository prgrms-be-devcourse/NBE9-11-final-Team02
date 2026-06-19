package com.back.sportteam.domain.match.repository;

import com.back.sportteam.domain.user.dto.request.MyMatchCondition;
import com.back.sportteam.domain.user.dto.response.MyMatchResponse;
import org.springframework.data.domain.Page;

public interface MatchParticipantQueryRepository {

    Page<MyMatchResponse> findMyMatches(String userId, MyMatchCondition condition);
}
