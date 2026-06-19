package com.back.sportteam.domain.user.dto.request;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.dto.MyMatchStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record MyMatchCondition(
        SportType sportType,
        MyMatchStatus myMatchStatus,
        MatchParticipantRole role,
        int page,
        int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "matchDate"));
    }
}
