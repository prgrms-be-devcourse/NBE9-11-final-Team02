package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.request.FacilityUpdateRequest;
import com.back.sportteam.domain.facility.dto.request.SlotSetupRequest;
import com.back.sportteam.domain.facility.dto.request.SlotUpdateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilitySlotResponse;
import com.back.sportteam.domain.facility.dto.response.FacilitySummaryResponse;
import com.back.sportteam.domain.facility.dto.response.FacilityReservationOverviewResponse;
import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityDetails;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.FacilityRevenueProjection;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.infra.s3.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilitySlotRepository facilitySlotRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private PaymentRepository paymentRepository;

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
    void 시설_상세_정보를_조회할_수_있다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        FacilityResponse response = facilityService.getFacility(facility.getId());

        assertThat(response.id()).isEqualTo(facility.getId());
        assertThat(response.name()).isEqualTo("테스트 풋살장");
    }

    @Test
    void 존재하지_않는_시설을_조회하면_예외가_발생한다() {
        when(facilityRepository.findByIdAndStatusNot("missing-id", FacilityStatus.CLOSED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getFacility("missing-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_NOT_FOUND);
    }

    @Test
    void 매니저는_본인_시설의_상세_정보를_조회할_수_있다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        FacilityResponse response = facilityService.getManagerFacility("manager-id", facility.getId());

        assertThat(response.id()).isEqualTo(facility.getId());
        assertThat(response.name()).isEqualTo("테스트 풋살장");
    }

    @Test
    void 다른_매니저는_시설_상세_정보를_조회할_수_없다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        String facilityId = facility.getId();

        assertThatThrownBy(() -> facilityService.getManagerFacility("other-manager-id", facilityId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 존재하지_않는_시설은_매니저가_상세_조회할_수_없다() {
        when(facilityRepository.findByIdAndStatusNot("missing-id", FacilityStatus.CLOSED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getManagerFacility("manager-id", "missing-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_NOT_FOUND);
    }

    @Test
    void 매니저는_본인이_등록한_시설_목록을_요약_정보로_조회한다() {
        Facility withImage = Facility.create(
                "manager-id", "이미지 있는 풋살장", "서울시 강남구",
                FacilityDetails.builder()
                        .phone("02-1234-5678")
                        .description("설명")
                        .capacity(20)
                        .slotDurationMinutes(60)
                        .defaultWeekdayPrice(50000)
                        .defaultWeekendPrice(70000)
                        .sportTypes(Set.of(SportType.FUTSAL))
                        .imageUrls(List.of("thumb.jpg", "second.jpg"))
                        .build()
        );
        Facility withoutImage = createFacility("manager-id");
        when(facilityRepository.findAllByManagerIdAndStatusNot("manager-id", FacilityStatus.CLOSED))
                .thenReturn(List.of(withImage, withoutImage));

        List<FacilitySummaryResponse> response = facilityService.getMyFacilities("manager-id");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).thumbnailUrl()).isEqualTo("thumb.jpg");
        assertThat(response.get(1).thumbnailUrl()).isNull();
    }

    @Test
    void 등록한_시설이_없으면_빈_목록을_반환한다() {
        when(facilityRepository.findAllByManagerIdAndStatusNot("manager-id", FacilityStatus.CLOSED))
                .thenReturn(List.of());

        List<FacilitySummaryResponse> response = facilityService.getMyFacilities("manager-id");

        assertThat(response).isEmpty();
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
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
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
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.updateFacility("other-manager-id", facilityId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 존재하지_않는_시설은_수정할_수_없다() {
        FacilityUpdateRequest request = new FacilityUpdateRequest(
                null, null, 20, 60, 50000, 70000, null, null, null, null
        );
        when(facilityRepository.findByIdAndStatusNot("missing-id", FacilityStatus.CLOSED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.updateFacility("manager-id", "missing-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_시설은_삭제할_수_없다() {
        when(facilityRepository.findByIdAndStatusNot("missing-id", FacilityStatus.CLOSED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.deleteFacility("manager-id", "missing-id"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_NOT_FOUND);
    }

    @Test
    void 예약된_슬롯이_없으면_시설을_삭제할_수_있다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndStatusIn(
                facility.getId(), List.of(SlotStatus.RESERVED, SlotStatus.PENDING)))
                .thenReturn(false);

        facilityService.deleteFacility("manager-id", facility.getId());

        assertThat(facility.getStatus()).isEqualTo(FacilityStatus.CLOSED);
    }

    @Test
    void 예약된_슬롯이_있으면_시설을_삭제할_수_없다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.existsByFacilityIdAndStatusIn(
                facility.getId(), List.of(SlotStatus.RESERVED, SlotStatus.PENDING)))
                .thenReturn(true);

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.deleteFacility("manager-id", facilityId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_HAS_ACTIVE_RESERVATIONS);
    }

    @Test
    void 본인_시설이_아니면_삭제할_수_없다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.deleteFacility("other-manager-id", facilityId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);

        verify(facilitySlotRepository, never()).existsByFacilityIdAndStatusIn(any(), any());
    }

    @Test
    void 슬롯을_설정하면_날짜_범위만큼_슬롯이_생성된다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 2),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(15, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findExistingSlotDatesByFacilityIdAndDateBetweenAndStartTime(any(), any(), any(), any()))
                .thenReturn(Set.of());
        when(facilitySlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FacilitySlotResponse> response = facilityService.setupSlots("manager-id", facility.getId(), request);

        assertThat(response).hasSize(12); // 2일 * 6슬롯(9-15, 1시간 단위)
        assertThat(response.getFirst().price()).isEqualTo(50000);
    }

    @Test
    void 영업_종료_시간이_시작_시간보다_빠르면_슬롯을_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(21, 0),
                java.time.LocalTime.of(9, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facilityId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_INVALID_TIME);
    }

    @Test
    void 시작시간과_종료시간이_같으면_슬롯을_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(9, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facilityId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_INVALID_TIME);
    }

    @Test
    void 시작_날짜가_종료_날짜보다_늦으면_슬롯을_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 2),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(21, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facilityId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_INVALID_DATE_RANGE);
    }

    @Test
    void 오늘이_속한_달로부터_3개월_뒤_달의_마지막_날을_초과하면_슬롯을_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        java.time.LocalDate tooFar = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
                .plusMonths(3)
                .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                .plusDays(1);
        SlotSetupRequest request = new SlotSetupRequest(
                tooFar,
                tooFar,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(21, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facilityId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_TOO_FAR_AHEAD);
    }

    @Test
    void 같은_날짜와_시작시간에_슬롯을_중복_생성할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(21, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findExistingSlotDatesByFacilityIdAndDateBetweenAndStartTime(any(), any(), any(), any()))
                .thenReturn(Set.of(java.time.LocalDate.of(2026, java.time.Month.JULY, 1)));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.setupSlots("manager-id", facilityId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_DUPLICATE);
    }

    @Test
    void 슬롯_가격과_상태를_수정할_수_있다() {
        Facility facility = createFacility("manager-id");
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId(slot.getId(), facility.getId()))
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
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId(slot.getId(), facility.getId()))
                .thenReturn(Optional.of(slot));

        // RESERVED 상태로 변경 시도
        SlotUpdateRequest invalidRequest = new SlotUpdateRequest(60000, SlotStatus.RESERVED);

        String facilityId = facility.getId();
        String slotId = slot.getId();
        assertThatThrownBy(() -> facilityService.updateSlot("manager-id", facilityId, slotId, invalidRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_INVALID_STATUS);
    }

    @Test
    void 요금_미입력시_시설_기본_요금으로_슬롯이_생성된다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                null,
                null
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findExistingSlotDatesByFacilityIdAndDateBetweenAndStartTime(any(), any(), any(), any()))
                .thenReturn(Set.of());
        when(facilitySlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FacilitySlotResponse> response = facilityService.setupSlots("manager-id", facility.getId(), request);

        // 7/1은 화요일(평일)이므로 기본 평일 요금 50000원 적용
        assertThat(response.getFirst().price()).isEqualTo(50000);
    }

    @Test
    void 주말_슬롯은_주말_요금으로_생성된다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 4), // 토요일
                java.time.LocalDate.of(2026, java.time.Month.JULY, 4),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                null,
                null
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findExistingSlotDatesByFacilityIdAndDateBetweenAndStartTime(any(), any(), any(), any()))
                .thenReturn(Set.of());
        when(facilitySlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<FacilitySlotResponse> response = facilityService.setupSlots("manager-id", facility.getId(), request);

        assertThat(response.getFirst().price()).isEqualTo(70000);
    }

    @Test
    void 본인_시설이_아니면_슬롯을_설정할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotSetupRequest request = new SlotSetupRequest(
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000,
                70000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.setupSlots("other-manager-id", facilityId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 이미_예약된_상태인_슬롯은_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                java.time.LocalDate.of(2026, java.time.Month.JULY, 1),
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        // 슬롯 자체를 RESERVED 상태로 만들기 위해 update 호출
        slot.update(50000, SlotStatus.RESERVED);

        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId(slot.getId(), facility.getId()))
                .thenReturn(Optional.of(slot));

        String facilityId = facility.getId();
        String slotId = slot.getId();
        assertThatThrownBy(() -> facilityService.updateSlot("manager-id", facilityId, slotId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_NOT_EDITABLE);
    }

    @Test
    void 존재하지_않는_슬롯은_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findByIdAndFacilityId("missing-slot-id", facility.getId()))
                .thenReturn(Optional.empty());

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.updateSlot("manager-id", facilityId, "missing-slot-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND);
    }

    @Test
    void 본인_시설이_아니면_슬롯을_수정할_수_없다() {
        Facility facility = createFacility("manager-id");
        SlotUpdateRequest request = new SlotUpdateRequest(60000, SlotStatus.AVAILABLE);

        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));

        String facilityId = facility.getId();
        assertThatThrownBy(() -> facilityService.updateSlot("other-manager-id", facilityId, "slot-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 날짜별_슬롯_목록을_조회할_수_있다() {
        Facility facility = createFacility("manager-id");
        java.time.LocalDate date = java.time.LocalDate.of(2026, java.time.Month.JULY, 1);
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                date,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(11, 0),
                50000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findAllByFacilityIdAndSlotDateOrderByStartTime(facility.getId(), date))
                .thenReturn(List.of(slot));

        List<FacilitySlotResponse> response = facilityService.getSlotsByDate(facility.getId(), date);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().startTime()).isEqualTo(java.time.LocalTime.of(9, 0));
        assertThat(response.getFirst().status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void 슬롯이_없는_날짜는_빈_목록을_반환한다() {
        Facility facility = createFacility("manager-id");
        java.time.LocalDate date = java.time.LocalDate.of(2026, java.time.Month.JULY, 1);

        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findAllByFacilityIdAndSlotDateOrderByStartTime(facility.getId(), date))
                .thenReturn(List.of());

        List<FacilitySlotResponse> response = facilityService.getSlotsByDate(facility.getId(), date);

        assertThat(response).isEmpty();
    }

    @Test
    void 매니저는_본인_시설의_날짜별_슬롯_목록을_조회할_수_있다() {
        Facility facility = createFacility("manager-id");
        LocalDate date = LocalDate.of(2026, Month.JULY, 1);
        FacilitySlot slot = FacilitySlot.create(
                facility.getId(),
                date,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                50000
        );
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findAllByFacilityIdAndSlotDateOrderByStartTime(facility.getId(), date))
                .thenReturn(List.of(slot));

        List<FacilitySlotResponse> response = facilityService.getManagerSlotsByDate("manager-id", facility.getId(), date);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.getFirst().status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void 다른_매니저는_시설의_슬롯_목록을_조회할_수_없다() {
        Facility facility = createFacility("manager-id");
        LocalDate date = LocalDate.of(2026, Month.JULY, 1);
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        String facilityId = facility.getId();

        assertThatThrownBy(() -> facilityService.getManagerSlotsByDate("other-manager-id", facilityId, date))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);

        verify(facilitySlotRepository, never()).findAllByFacilityIdAndSlotDateOrderByStartTime(any(), any());
    }

    @Test
    void 시설_예약_현황과_수익과_잔여_타임을_조회한다() {
        Facility facility = createFacility("manager-id");
        LocalDate date = LocalDate.of(2026, Month.JULY, 1);
        FacilitySlot reservedSlot = FacilitySlot.create(
                facility.getId(), date, LocalTime.of(9, 0), LocalTime.of(10, 0), 50_000
        );
        reservedSlot.reserve();
        FacilitySlot availableSlot = FacilitySlot.create(
                facility.getId(), date, LocalTime.of(10, 0), LocalTime.of(11, 0), 50_000
        );
        Reservation reservation = Reservation.pending(
                reservedSlot.getId(),
                LocalDateTime.of(2026, Month.JUNE, 20, 10, 0)
        );
        reservation.confirm();
        FacilityRevenueProjection revenue = mock(FacilityRevenueProjection.class);

        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        when(facilitySlotRepository.findAllByFacilityIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
                facility.getId(), date, date
        )).thenReturn(List.of(reservedSlot, availableSlot));
        when(reservationRepository.findAllByFacilitySlotIdIn(
                List.of(reservedSlot.getId(), availableSlot.getId())
        )).thenReturn(List.of(reservation));
        when(paymentRepository.sumNetRevenueByFacilitySlotIds(
                List.of(reservedSlot.getId(), availableSlot.getId()),
                PaymentType.FACILITY,
                List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED)
        )).thenReturn(List.of(revenue));
        when(revenue.getFacilitySlotId()).thenReturn(reservedSlot.getId());
        when(revenue.getNetRevenue()).thenReturn(45_000L);

        FacilityReservationOverviewResponse response = facilityService.getReservations(
                "manager-id", facility.getId(), date, date
        );

        assertThat(response.totalSlots()).isEqualTo(2);
        assertThat(response.reservedSlots()).isEqualTo(1);
        assertThat(response.availableSlots()).isEqualTo(1);
        assertThat(response.totalRevenue()).isEqualTo(45_000L);
        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots().getFirst().reservationId()).isEqualTo(reservation.getId());
        assertThat(response.slots().getFirst().revenue()).isEqualTo(45_000L);
    }

    @Test
    void 다른_매니저는_시설_예약_현황을_조회할_수_없다() {
        Facility facility = createFacility("manager-id");
        LocalDate date = LocalDate.of(2026, Month.JULY, 1);
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        String facilityId = facility.getId();

        assertThatThrownBy(() -> facilityService.getReservations(
                "other-manager-id", facilityId, date, date
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_ACCESS_DENIED);
    }

    @Test
    void 예약_조회_시작일이_종료일보다_늦으면_예외가_발생한다() {
        Facility facility = createFacility("manager-id");
        LocalDate fromDate = LocalDate.of(2026, Month.JULY, 2);
        LocalDate toDate = LocalDate.of(2026, Month.JULY, 1);
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        String facilityId = facility.getId();

        assertThatThrownBy(() -> facilityService.getReservations(
                "manager-id", facilityId, fromDate, toDate
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_RESERVATION_INVALID_DATE_RANGE);
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

    @Test
    void 이미지_삭제_시_S3와_시설_엔티티에서_모두_제거된다() {
        String imageUrl = "https://team02-bucket-8282.s3.ap-northeast-2.amazonaws.com/facilities/uuid";
        Facility facility = createFacilityWithImage("manager-id", imageUrl);
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        doNothing().when(s3Service).deleteFile(imageUrl);

        facilityService.deleteImage("manager-id", facility.getId(), imageUrl);

        verify(s3Service).deleteFile(imageUrl);
        assertThat(facility.getImageUrls()).doesNotContain(imageUrl);
    }

    @Test
    void 시설에_등록되지_않은_이미지_삭제_시_예외가_발생한다() {
        Facility facility = createFacility("manager-id");
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        String facilityId = facility.getId();

        assertThatThrownBy(() -> facilityService.deleteImage("manager-id", facilityId, "https://other-url/img.jpg"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_IMAGE_NOT_FOUND);
        verify(s3Service, never()).deleteFile(anyString());
    }

    @Test
    void S3_삭제_실패_시_FACILITY_IMAGE_DELETE_FAILED_예외가_발생한다() {
        String imageUrl = "https://team02-bucket-8282.s3.ap-northeast-2.amazonaws.com/facilities/uuid";
        Facility facility = createFacilityWithImage("manager-id", imageUrl);
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        doThrow(new RuntimeException("S3 error")).when(s3Service).deleteFile(imageUrl);
        String facilityId = facility.getId();

        assertThatThrownBy(() -> facilityService.deleteImage("manager-id", facilityId, imageUrl))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FacilityErrorCode.FACILITY_IMAGE_DELETE_FAILED);
    }

    @Test
    void 시설_수정_시_리스트에서_빠진_이미지는_S3에서도_정리된다() {
        String removed = "https://team02-bucket-8282.s3.ap-northeast-2.amazonaws.com/facilities/removed";
        String kept = "https://team02-bucket-8282.s3.ap-northeast-2.amazonaws.com/facilities/kept";
        Facility facility = createFacilityWithImages("manager-id", List.of(removed, kept));
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        FacilityUpdateRequest request = new FacilityUpdateRequest(
                null, null, 20, 60, 50000, 70000, null, null, null, List.of(kept)
        );

        facilityService.updateFacility("manager-id", facility.getId(), request);

        verify(s3Service).deleteFile(removed);
        verify(s3Service, never()).deleteFile(kept);
        assertThat(facility.getImageUrls()).containsExactly(kept);
    }

    @Test
    void 시설_수정_중_S3_정리에_실패해도_수정은_완료된다() {
        String removed = "https://team02-bucket-8282.s3.ap-northeast-2.amazonaws.com/facilities/removed";
        Facility facility = createFacilityWithImages("manager-id", List.of(removed));
        when(facilityRepository.findByIdAndStatusNot(facility.getId(), FacilityStatus.CLOSED))
                .thenReturn(Optional.of(facility));
        doThrow(new RuntimeException("S3 error")).when(s3Service).deleteFile(removed);
        FacilityUpdateRequest request = new FacilityUpdateRequest(
                "02-0000-0000", null, 20, 60, 50000, 70000, null, null, null, List.of()
        );

        FacilityResponse response = facilityService.updateFacility("manager-id", facility.getId(), request);

        assertThat(response.phone()).isEqualTo("02-0000-0000");
        assertThat(facility.getImageUrls()).isEmpty();
        verify(s3Service).deleteFile(removed);
    }

    private Facility createFacility(String managerId) {
        return Facility.create(
                managerId,
                "테스트 풋살장",
                "서울시 강남구",
                FacilityDetails.builder()
                        .phone("02-1234-5678")
                        .description("테스트 시설입니다.")
                        .capacity(20)
                        .slotDurationMinutes(60)
                        .defaultWeekdayPrice(50000)
                        .defaultWeekendPrice(70000)
                        .sportTypes(Set.of(SportType.FUTSAL))
                        .build()
        );
    }

    private Facility createFacilityWithImage(String managerId, String imageUrl) {
        return createFacilityWithImages(managerId, List.of(imageUrl));
    }

    private Facility createFacilityWithImages(String managerId, List<String> imageUrls) {
        List<String> images = new ArrayList<>(imageUrls);
        return Facility.create(
                managerId,
                "테스트 풋살장",
                "서울시 강남구",
                FacilityDetails.builder()
                        .phone("02-1234-5678")
                        .description("테스트 시설입니다.")
                        .capacity(20)
                        .slotDurationMinutes(60)
                        .defaultWeekdayPrice(50000)
                        .defaultWeekendPrice(70000)
                        .sportTypes(Set.of(SportType.FUTSAL))
                        .imageUrls(images)
                        .build()
        );
    }
}
