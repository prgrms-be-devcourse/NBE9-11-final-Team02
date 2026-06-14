package com.back.sportteam.domain.match.dto.request;

import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MatchCreateRequest(
        @NotBlank(message = "예약 ID는 필수입니다.")
        String reservationId,

        @NotBlank(message = "매칭방 제목은 필수입니다.")
        @Size(max = 100, message = "매칭방 제목은 100자 이하여야 합니다.")
        String title,

        @NotNull(message = "종목은 필수입니다.")
        SportType sportType,

        @Min(value = 1, message = "최소 인원은 1명 이상이어야 합니다.")
        int minParticipants,

        @Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
        int maxParticipants,

        @Min(value = 0, message = "1인 분담금은 0원 이상이어야 합니다.")
        int feePerPerson,

        @NotNull(message = "최소 실력 조건을 선택해주세요.")
        SkillLevel minSkillLevel,

        @NotNull(message = "최대 실력 조건을 선택해주세요.")
        SkillLevel maxSkillLevel,

        @NotNull(message = "성별 조건을 선택해주세요.")
        RequiredGender requiredGender,

        @NotNull(message = "모집 마감 시간은 필수입니다.")
        @Future(message = "모집 마감 시간은 현재 시간 이후여야 합니다.")
        LocalDateTime recruitDeadline,

        @NotNull(message = "취소 마감 시간은 필수입니다.")
        @Future(message = "취소 마감 시간은 현재 시간 이후여야 합니다.")
        LocalDateTime cancelDeadline
) {
}
