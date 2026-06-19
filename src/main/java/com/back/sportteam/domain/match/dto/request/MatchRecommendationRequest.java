package com.back.sportteam.domain.match.dto.request;

import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatchRecommendationRequest(
        @NotNull(message = "종목은 필수입니다.")
        SportType sportType,

        RequiredGender gender,

        @Min(value = 1, message = "추천 개수는 1개 이상이어야 합니다.")
        @Max(value = 20, message = "추천 개수는 최대 20개까지 가능합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 5;

    public int recommendationSize() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
