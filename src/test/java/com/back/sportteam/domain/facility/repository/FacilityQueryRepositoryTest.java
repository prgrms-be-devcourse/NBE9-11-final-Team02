package com.back.sportteam.domain.facility.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityDetails;
import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.global.config.QueryDslConfig;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({QueryDslConfig.class, FacilityQueryRepository.class})
@ActiveProfiles("test")
class FacilityQueryRepositoryTest {

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private FacilitySlotRepository facilitySlotRepository;

    @Autowired
    private FacilityQueryRepository facilityQueryRepository;

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, Month.JUNE, 10);

    @BeforeEach
    void setUp() {
        Facility futsal = facilityRepository.save(Facility.create(
                "manager-1", "창원 풋살장", "창원시 성산구",
                details(Set.of(SportType.FUTSAL))
        ));
        facilitySlotRepository.save(FacilitySlot.create(
                futsal.getId(), TARGET_DATE, LocalTime.of(10, 0), LocalTime.of(12, 0), 10_000
        ));

        Facility tennis = facilityRepository.save(Facility.create(
                "manager-2", "마산 테니스장", "창원시 마산합포구",
                details(Set.of(SportType.TENNIS))
        ));
        facilitySlotRepository.save(FacilitySlot.create(
                tennis.getId(), TARGET_DATE, LocalTime.of(14, 0), LocalTime.of(16, 0), 20_000
        ));
    }

    @DisplayName("종목 조건으로 시설 필터링")
    @Test
    void 종목_조건으로_시설_필터링() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Facility> result = facilityQueryRepository.findAvailable(
                SportType.FUTSAL, null, null, pageable
        );

        assertThat(result.getContent())
                .extracting(Facility::getName)
                .containsExactly("창원 풋살장");
    }

    @DisplayName("지역 조건으로 시설을 필터링한다")
    @Test
    void 지역_조건으로_시설_필터링() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Facility> result = facilityQueryRepository.findAvailable(
                null, "마산", null, pageable
        );

        assertThat(result.getContent())
                .extracting(Facility::getName)
                .containsExactly("마산 테니스장");
    }

    @DisplayName("날짜 조건으로 AVAILABLE 슬롯이 있는 시설만 조회")
    @Test
    void 날짜_조건으로_AVAILABLE_슬롯이_있는_시설만_조회() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Facility> result = facilityQueryRepository.findAvailable(
                null, null, TARGET_DATE, pageable
        );

        assertThat(result.getContent()).hasSize(2);
    }

    @DisplayName("조건이 없으면 ACTIVE 시설을 전체 조회")
    @Test
    void 조건이_없으면_ACTIVE_시설을_전체_조회() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Facility> result = facilityQueryRepository.findAvailable(
                null, null, null, pageable
        );

        assertThat(result.getContent()).hasSize(2);
    }

    private FacilityDetails details(Set<SportType> sportTypes) {
        return FacilityDetails.builder()
                .phone("010-1234-5678")
                .description("설명")
                .capacity(20)
                .slotDurationMinutes(60)
                .defaultWeekdayPrice(10_000)
                .defaultWeekendPrice(20_000)
                .slotOpenAt(null)
                .sportTypes(sportTypes)
                .amenities(Set.of())
                .imageUrls(List.of("https://img.com/1.png"))
                .build();
    }
}