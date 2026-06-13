package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.match.entity.SportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @InjectMocks
    private FacilityService facilityService;

    @Test
    void 시설을_등록하면_ACTIVE_상태로_저장된다() {
        FacilityCreateRequest request = createRequest();
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FacilityResponse response = facilityService.createFacility("manager-id", request);

        assertThat(response.name()).isEqualTo("테스트 풋살장");
        assertThat(response.address()).isEqualTo("서울시 강남구");
        assertThat(response.status()).isEqualTo(FacilityStatus.ACTIVE);
        assertThat(response.id()).isNotBlank();
        verify(facilityRepository).save(any(Facility.class));
    }

    private FacilityCreateRequest createRequest() {
        return new FacilityCreateRequest(
                "테스트 풋살장",
                "서울시 강남구",
                "02-1234-5678",
                "테스트 시설입니다.",
                60,
                10,
                null,
                Set.of(SportType.FUTSAL),
                null,
                null
        );
    }
}
