package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.request.FacilityUpdateRequest;
import com.back.sportteam.domain.facility.dto.request.SlotSetupRequest;
import com.back.sportteam.domain.facility.dto.request.SlotUpdateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilitySlotResponse;
import com.back.sportteam.domain.facility.entity.FacilitySlot;
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
                20,
                90,
                50000,
                70000,
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
                null, null, 20, 60, 50000, 70000, null, null, null, null
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
                null, null, 20, 60, 50000, 70000, null, null, null, null
        );
        when(facilityRepository.findByIdAndStatusNot(eq("missing-id"), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.updateFacility("manager-id", "missing-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_시설은_삭제할_수_없다() {
        when(facilityRepository.findByIdAndStatusNot(eq("missing-id"), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.deleteFacility("manager-id", "missing-id"))
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

    @Test
    void 슬롯을_설정하면_날짜_범위만큼_슬롯이_생성된다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 2),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(15, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndSlotDateAndStartTime(any(), any(), any()))
                .thenReturn(false);
        when(facilitySlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FacilitySlotResponse> response = facilityService.setupSlots("manager-id", facility.getId(), request);

        assertThat(response).hasSize(12); // 2일 * 6슬롯(9-15, 1시간 단위)
        assertThat(response.getFirst().price()).isEqualTo(50000);
    }

    @Test
    void 영업_종료_시간이_시작_시간보다_빠르면_슬롯을_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(21, 0),
                java.time.LocalTime.of(9, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facility.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_INVALID_TIME);
    }

    @Test
    void 시작시간과_종료시간이_같으면_슬롯을_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(9, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facility.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_INVALID_TIME);
    }

    @Test
    void 같은_날짜와_시작시간에_슬롯을_중복_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(21, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndSlotDateAndStartTime(any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facility.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_DUPLICATE);
    }

    @Test
    void 슬롯_가격과_상태를_수정할_수_있다() {
        Facility facility = createFacility("manager-id");
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId(eq(slot.getId()), eq(facility.getId())))
                .thenReturn(Optional.of(slot));

        FacilitySlotResponse response = facilityService.updateSlot("manager-id", facility.getId(), slot.getId(), request);

        assertThat(response.price()).isEqualTo(60000);
        assertThat(response.status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void 예약_중인_슬롯은_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId(eq(slot.getId()), eq(facility.getId())))
                .thenReturn(Optional.of(slot));

        // RESERVED 상태로 변경 시도
        SlotUpdateRequest invalidRequest = new SlotUpdateRequest(60000, SlotStatus.RESERVED);

        assertThatThrownBy(() -> facilityService.updateSlot("manager-id", facility.getId(), slot.getId(), invalidRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_INVALID_STATUS);
    }

    @Test
    void 요금_미입력시_시설_기본_요금으로_슬롯이_생성된다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                null,
                null
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndSlotDateAndStartTime(any(), any(), any()))
                .thenReturn(false);
        when(facilitySlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FacilitySlotResponse> response = facilityService.setupSlots("manager-id", facility.getId(), request);

        // 7/1은 화요일(평일)이므로 기본 평일 요금 50000원 적용
        assertThat(response.getFirst().price()).isEqualTo(50000);
    }

    @Test
    void 주말_슬롯은_주말_요금으로_생성된다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, 7, 4), // 토요일
                java.time.LocalDate.of(2026, 7, 4),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                null,
                null
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndSlotDateAndStartTime(any(), any(), any()))
                .thenReturn(false);
        when(facilitySlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FacilitySlotResponse> response = facilityService.setupSlots("manager-id", facility.getId(), request);

        assertThat(response.getFirst().price()).isEqualTo(70000);
    }

    @Test
    void 본인_시설이_아니면_슬롯을_설정할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.setupSlots("other-manager-id", facility.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 이미_예약된_상태인_슬롯은_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        // 슬롯 자체를 RESERVED 상태로 만들기 위해 update 호출
        slot.update(50000, SlotStatus.RESERVED);

        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId(eq(slot.getId()), eq(facility.getId())))
                .thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> facilityService.updateSlot("manager-id", facility.getId(), slot.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_NOT_EDITABLE);
    }

    @Test
    void 존재하지_않는_슬롯은_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId(eq("missing-slot-id"), eq(facility.getId())))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.updateSlot("manager-id", facility.getId(), "missing-slot-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND);
    }

    @Test
    void 본인_시설이_아니면_슬롯을_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> facilityService.updateSlot("other-manager-id", facility.getId(), "slot-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 날짜별_슬롯_목록을_조회할_수_있다() {
        Facility facility = createFacility("manager-id");
        java.time.LocalDate date = java.time.LocalDate.of(2026, 7, 1);
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                date,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findAllByFacilityIdAndSlotDateOrderByStartTime(eq(facility.getId()), eq(date)))
                .thenReturn(List.of(slot));

        List<FacilitySlotResponse> response = facilityService.getSlotsByDate(facility.getId(), date);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().startTime()).isEqualTo(java.time.LocalTime.of(9, 0));
        assertThat(response.getFirst().status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void 슬롯이_없는_날짜는_빈_목록을_반환한다() {
        Facility facility = createFacility("manager-id");
        java.time.LocalDate date = java.time.LocalDate.of(2026, 7, 1);

        when(facilityRepository.findByIdAndStatusNot(eq(facility.getId()), eq(FacilityStatus.CLOSED)))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findAllByFacilityIdAndSlotDateOrderByStartTime(eq(facility.getId()), eq(date)))
                .thenReturn(List.of());

        List<FacilitySlotResponse> response = facilityService.getSlotsByDate(facility.getId(), date);

        assertThat(response).isEmpty();
    }

    private FacilityCreateRequest createRequest() {
        return new FacilityCreateRequest(
                "테스트 풋살장",
                "서울시 강남구",
                "02-1234-5678",
                "테스트 시설입니다.",
                20,
                60,
                50000,
                70000,
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
                20,
                60,
                50000,
                70000,
                null,
                Set.of(SportType.FUTSAL),
                null,
                null
        );
    }
}
