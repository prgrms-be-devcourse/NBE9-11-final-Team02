package com.back.sportteam.domain.facility.dto.request;

import com.back.sportteam.domain.facility.entity.SlotStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SlotUpdateRequest(
        @NotNull
        @Min(value = 0, message = "요금은 0원 이상이어야 합니다.")
        Integer price,

        @NotNull(message = "슬롯 상태는 필수입니다.")
        SlotStatus status
) {
}
