package com.back.sportteam.domain.match.dto.request;

import org.springframework.data.domain.Sort;

public enum MatchSortType {
    LATEST,
    DEADLINE_ASC,
    FEE_ASC,
    PARTICIPANT_DESC;

    public Sort toSort() {
        return switch (this) {
            case LATEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case DEADLINE_ASC -> Sort.by(Sort.Order.asc("recruitDeadline"), Sort.Order.asc("id"));
            case FEE_ASC -> Sort.by(Sort.Order.asc("feePerPerson"), Sort.Order.asc("id"));
            case PARTICIPANT_DESC -> Sort.by(Sort.Order.desc("currentCount"), Sort.Order.desc("id"));
        };
    }
}
