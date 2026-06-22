package com.back.sportteam.domain.facility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.facility.dto.response.FacilityAvailableResponse;
import com.back.sportteam.domain.facility.repository.FacilityQueryRepository;
import com.back.sportteam.domain.match.entity.SportType;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class FacilityAvailableServiceTest {

    private FacilityQueryRepository facilityQueryRepository;
    private FacilityAvailableService facilityAvailableService;

    @BeforeEach
    void setUp() {
        facilityQueryRepository = mock(FacilityQueryRepository.class);
        facilityAvailableService = new FacilityAvailableService(facilityQueryRepository);
    }

    @DisplayName("조건 없이 전체 예약 가능 시설 목록 조회")
    @Test
    void 조건_없이_전체_예약_가능_시설_목록_조회() {
        Pageable pageable = PageRequest.of(0, 20);
        when(facilityQueryRepository.findAvailable(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FacilityAvailableResponse> result =
                facilityAvailableService.getAvailableFacilities(null, null, null, pageable);

        assertThat(result).isEmpty();
        verify(facilityQueryRepository).findAvailable(null, null, null, pageable);
    }

    @DisplayName("종목, 지역, 날짜 조건으로 시설 목록을 조회")
    @Test
    void 종목_지역_날짜_조건으로_시설_목록_조회() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate date = LocalDate.of(2026, Month.JUNE, 10);

        when(facilityQueryRepository.findAvailable(SportType.FUTSAL, "서울", date, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FacilityAvailableResponse> result =
                facilityAvailableService.getAvailableFacilities(SportType.FUTSAL, "서울", date, pageable);

        assertThat(result).isEmpty();
        verify(facilityQueryRepository).findAvailable(SportType.FUTSAL, "서울", date, pageable);
    }
}