package com.back.sportteam.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 100, message = "닉네임은 100자 이하입니다.")
        String nickname,

        @Size(max = 20, message = "포지션은 20자 이하입니다.")
        String position,

        @Size(max = 100, message = "주 활동 지역은 100자 이하입니다.")
        String activeRegion,

        @Size(max = 50, message = "선호 종목은 50자 이하입니다.")
        String preferredSport,

        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하입니다.")
        String profileImg
) {

}