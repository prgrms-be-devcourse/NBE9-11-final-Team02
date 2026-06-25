package com.back.sportteam.domain.facility.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityDetails;
import com.back.sportteam.domain.match.entity.SportType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FacilityAvailableResponseTest {

    @DisplayName("이미지가 있을 시 첫 번째 이미지를 썸네일로 사용")
    @Test
    void 이미지가_있을_시_첫_번째_이미지를_썸네일로_사용() {
        Facility facility = Facility.create(
                "manager-id",
                "창원 풋살장",
                "창원시 성산구 원이대로 123",
                details(List.of("https://img.com/1.png", "https://img.com/2.png"))
        );

        FacilityAvailableResponse response = FacilityAvailableResponse.from(facility);

        assertThat(response.facilityId()).isEqualTo(facility.getId());
        assertThat(response.name()).isEqualTo("창원 풋살장");
        assertThat(response.address()).isEqualTo("창원시 성산구 원이대로 123");
        assertThat(response.thumbnailUrl()).isEqualTo("https://img.com/1.png");
        assertThat(response.sportTypes()).containsExactly(SportType.FUTSAL);
    }

    @DisplayName("이미지가 없을 시 썸네일은 null")
    @Test
    void 이미지가_없을_시_썸네일은_null() {
        Facility facility = Facility.create(
                "manager-id",
                "이미지 없는 시설",
                "서울시 송파구",
                details(List.of())
        );

        FacilityAvailableResponse response = FacilityAvailableResponse.from(facility);

        assertThat(response.thumbnailUrl()).isNull();
        assertThat(response.defaultWeekdayPrice()).isEqualTo(10_000);
        assertThat(response.defaultWeekendPrice()).isEqualTo(20_000);
        assertThat(response.ratingAvg()).isEqualTo(0.0);
        assertThat(response.reviewCount()).isZero();
    }

    @DisplayName("Redis 캐시 저장을 위해 직렬화 가능")
    @Test
    void Redis_캐시_저장을_위해_직렬화_가능() throws IOException {
        Facility facility = Facility.create(
                "manager-id",
                "캐시 테스트 시설",
                "서울시 강남구",
                details(List.of("https://img.com/cache.png"))
        );
        FacilityAvailableResponse response = FacilityAvailableResponse.from(facility);

        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            objectOutputStream.writeObject(response);
        }
    }

    private FacilityDetails details(List<String> imageUrls) {
        return FacilityDetails.builder()
                .phone("010-1234-5678")
                .description("설명")
                .capacity(20)
                .slotDurationMinutes(60)
                .defaultWeekdayPrice(10_000)
                .defaultWeekendPrice(20_000)
                .slotOpenAt(null)
                .sportTypes(Set.of(SportType.FUTSAL))
                .amenities(Set.of())
                .imageUrls(imageUrls)
                .build();
    }
}
