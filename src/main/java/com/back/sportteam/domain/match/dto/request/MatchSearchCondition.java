package com.back.sportteam.domain.match.dto.request;

import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record MatchSearchCondition(
        SportType sportType,
        MatchStatus status,
        SkillLevel minSkillLevel,
        SkillLevel maxSkillLevel,
        RequiredGender requiredGender,
        MatchSortType sort,
        int page,
        int size
) {

    private static final MatchSortType DEFAULT_SORT = MatchSortType.LATEST;

    public Pageable toPageable() {
        MatchSortType sortType = sort != null ? sort : DEFAULT_SORT;
        return PageRequest.of(page, size, sortType.toSort());
    }
}
