package com.back.sportteam.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserRestrictionRequest(
        @NotNull(message = "제한 여부는 필수입니다.")
        Boolean restricted,

        @Size(max = 500, message = "제한 사유는 500자 이하여야 합니다.")
        String reason
) {
}
