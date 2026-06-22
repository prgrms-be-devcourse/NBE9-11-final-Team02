package com.back.sportteam.domain.facility.dto.response;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.match.entity.SportType;
import java.util.Set;

public record FacilityAvailableResponse(
        String facilityId,
        String name,
        String address,
        int defaultWeekdayPrice,
        int defaultWeekendPrice,
        Set<SportType> sportTypes,
        String thumbnailUrl,
        double ratingAvg,
        int reviewCount
) {
    public static FacilityAvailableResponse from(Facility facility) {
        String thumbnail = facility.getImageUrls().isEmpty()
                ? null
                : facility.getImageUrls().get(0);
        return new FacilityAvailableResponse(
                facility.getId(),
                facility.getName(),
                facility.getAddress(),
                facility.getDefaultWeekdayPrice(),
                facility.getDefaultWeekendPrice(),
                facility.getSportTypes(),
                thumbnail,
                facility.getRatingAvg().doubleValue(),
                facility.getReviewCount()
        );
    }
}