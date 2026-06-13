package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.request.FacilityUpdateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilitySlotRepository facilitySlotRepository;

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

    @Test
    void 시설을_수정하면_변경된_정보가_반환된다() {
        Facility facility = createFacility("manager-id");
        FacilityUpdateRequest request = new FacilityUpdateRequest(
                "02-9876-5432",
                "수정된 설명입니다.",
                90,
                8,
                null,
                null,
                null,
                null
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));

        FacilityResponse response = facilityService.updateFacility("manager-id", facility.getId(), request);

        assertThat(response.phone()).isEqualTo("02-9876-5432");
        assertThat(response.slotDurationMinutes()).isEqualTo(90);
    }

    @Test
    void 본인_시설이_아니면_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        FacilityUpdateRequest request = new FacilityUpdateRequest(
                null, null, 60, 10, null, null, null, null
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.updateFacility("other-manager-id", facility.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 존재하지_않는_시설은_수정할_수_없다() {
        FacilityUpdateRequest request = new FacilityUpdateRequest(
                null, null, 60, 10, null, null, null, null
        );
        when(facilityRepository.findByIdAndStatusNot(eq("missing-id"), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.updateFacility("manager-id", "missing-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_NOT_FOUND);
    }

    @Test
    void 예약된_슬롯이_없으면_시설을_삭제할_수_있다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndStatusIn(
                eq(facility.getId()), eq(List.of(SlotStatus.RESERVED, SlotStatus.PENDING))))
                .thenReturn(false);

        facilityService.deleteFacility("manager-id", facility.getId());

        assertThat(facility.getStatus()).isEqualTo(FacilityStatus.CLOSED);
    }

    @Test
    void 예약된_슬롯이_있으면_시설을_삭제할_수_없다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndStatusIn(
                eq(facility.getId()), eq(List.of(SlotStatus.RESERVED, SlotStatus.PENDING))))
                .thenReturn(true);

        assertThatThrownBy(() -> facilityService.deleteFacility("manager-id", facility.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_HAS_ACTIVE_RESERVATIONS);
    }

    @Test
    void 본인_시설이_아니면_삭제할_수_없다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.deleteFacility("other-manager-id", facility.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);

        verify(facilitySlotRepository, never()).existsByFacilityIdAndStatusIn(any(), any());
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

    private Facility createFacility(String managerId) {
        return Facility.create(
                managerId,
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
