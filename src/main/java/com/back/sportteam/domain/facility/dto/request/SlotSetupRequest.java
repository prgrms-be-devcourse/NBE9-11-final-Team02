package com.back.sportteam.domain.facility.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotSetupRequest(
        @NotNull(message = "시작 날짜는 필수입니다.")
        LocalDate fromDate,

        @NotNull(message = "종료 날짜는 필수입니다.")
        LocalDate toDate,

        @NotNull(message = "영업 시작 시간은 필수입니다.")
        LocalTime startTime,

        @NotNull(message = "영업 종료 시간은 필수입니다.")
        LocalTime endTime,

        @Min(value = 0, message = "평일 요금은 0원 이상이어야 합니다.")
        Integer weekdayPrice,

        @Min(value = 0, message = "주말 요금은 0원 이상이어야 합니다.")
        Integer weekendPrice
) {
}
