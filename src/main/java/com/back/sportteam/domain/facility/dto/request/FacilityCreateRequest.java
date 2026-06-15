package com.back.sportteam.domain.facility.dto.request;

import com.back.sportteam.domain.facility.entity.Amenity;
import com.back.sportteam.domain.match.entity.SportType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record FacilityCreateRequest(
        @NotBlank(message = "시설명은 필수입니다.")
        String name,

        @NotBlank(message = "주소는 필수입니다.")
        String address,

        String phone,

        String description,

        @NotNull
        @Min(value = 1, message = "수용 인원은 1명 이상이어야 합니다.")
        Integer capacity,

        @NotNull
        @Min(value = 30, message = "슬롯 단위는 최소 30분입니다.")
        Integer slotDurationMinutes,

        @NotNull
        @Min(value = 0, message = "평일 기본 요금은 0원 이상이어야 합니다.")
        Integer defaultWeekdayPrice,

        @NotNull
        @Min(value = 0, message = "주말 기본 요금은 0원 이상이어야 합니다.")
        Integer defaultWeekendPrice,

        LocalDateTime slotOpenAt,

        @NotEmpty(message = "지원 종목을 1개 이상 선택해야 합니다.")
        Set<SportType> sportTypes,

        Set<Amenity> amenities,

        List<String> imageUrls
) {
}
