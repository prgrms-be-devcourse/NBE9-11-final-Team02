package com.back.sportteam.domain.user.dto.request;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.SelfReportedLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SportStatRegisterRequest(
        @NotNull @NotEmpty List<@Valid SportStatItem> stats
) {
    public record SportStatItem(
            @NotNull SportType sportType,
            @NotNull SelfReportedLevel selfReportedLevel
    ) {}
}
