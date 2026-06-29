package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.request.FacilityUpdateRequest;
import com.back.sportteam.domain.facility.dto.request.SlotSetupRequest;
import com.back.sportteam.domain.facility.dto.request.SlotUpdateRequest;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.dto.response.FacilityReservationOverviewResponse;
import com.back.sportteam.domain.facility.dto.response.FacilityReservationSlotResponse;
import com.back.sportteam.domain.facility.dto.response.FacilitySummaryResponse;
import com.back.sportteam.domain.facility.dto.response.FacilitySlotResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityDetails;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.FacilityRevenueProjection;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacilityService {

    private static final int MAX_MONTHS_AHEAD = 3;
    private static final List<SlotStatus> PRICE_UPDATABLE_STATUSES = List.of(SlotStatus.AVAILABLE, SlotStatus.CLOSED);

    private final FacilityRepository facilityRepository;
    private final FacilitySlotRepository facilitySlotRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final com.back.sportteam.infra.s3.S3Service s3Service;

    @Transactional
    public FacilityResponse createFacility(String managerId, FacilityCreateRequest request) {
        Facility facility = Facility.create(
                managerId,
                request.name(),
                request.address(),
                FacilityDetails.builder()
                        .phone(request.phone())
                        .description(request.description())
                        .capacity(request.capacity())
                        .slotDurationMinutes(request.slotDurationMinutes())
                        .defaultWeekdayPrice(request.defaultWeekdayPrice())
                        .defaultWeekendPrice(request.defaultWeekendPrice())
                        .slotOpenAt(request.slotOpenAt())
                        .sportTypes(request.sportTypes())
                        .amenities(request.amenities())
                        .imageUrls(request.imageUrls())
                        .build()
        );
        return FacilityResponse.from(facilityRepository.save(facility));
    }

    @Transactional(readOnly = true)
    public FacilityResponse getFacility(String facilityId) {
        return FacilityResponse.from(getFacilityOrThrow(facilityId));
    }

    @Transactional(readOnly = true)
    public List<FacilitySummaryResponse> getMyFacilities(String managerId) {
        return facilityRepository.findAllByManagerIdAndStatusNot(managerId, FacilityStatus.CLOSED)
                .stream()
                .map(FacilitySummaryResponse::from)
                .toList();
    }

    @Transactional
    public FacilityResponse updateFacility(String managerId, String facilityId, FacilityUpdateRequest request) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);

        List<String> removedImageUrls = resolveRemovedImageUrls(facility.getImageUrls(), request.imageUrls());

        facility.update(
                FacilityDetails.builder()
                        .phone(request.phone())
                        .description(request.description())
                        .capacity(request.capacity())
                        .slotDurationMinutes(request.slotDurationMinutes())
                        .defaultWeekdayPrice(request.defaultWeekdayPrice())
                        .defaultWeekendPrice(request.defaultWeekendPrice())
                        .slotOpenAt(request.slotOpenAt())
                        .sportTypes(request.sportTypes())
                        .amenities(request.amenities())
                        .imageUrls(request.imageUrls())
                        .build()
        );

        facilitySlotRepository.updateWeekdayPrice(facilityId, PRICE_UPDATABLE_STATUSES, request.defaultWeekdayPrice());
        facilitySlotRepository.updateWeekendPrice(facilityId, PRICE_UPDATABLE_STATUSES, request.defaultWeekendPrice());

        cleanupRemovedImages(removedImageUrls);

        return FacilityResponse.from(facility);
    }

    private List<String> resolveRemovedImageUrls(List<String> current, List<String> updated) {
        // null은 엔티티 갱신과 동일하게 전체 비움으로 해석한다.
        List<String> next = updated != null ? updated : List.of();
        return current.stream()
                .filter(url -> !next.contains(url))
                .toList();
    }

    private void cleanupRemovedImages(List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            try {
                s3Service.deleteFile(imageUrl);
            } catch (Exception e) {
                log.warn("[Facility] S3 이미지 정리 실패 - 고아 객체 발생 가능. imageUrl={}", imageUrl, e);
            }
        }
    }

    @Transactional
    public void deleteImage(String managerId, String facilityId, String imageUrl) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);

        if (!facility.getImageUrls().contains(imageUrl)) {
            throw new BusinessException(FacilityErrorCode.FACILITY_IMAGE_NOT_FOUND);
        }

        try {
            s3Service.deleteFile(imageUrl);
        } catch (Exception _) {
            throw new BusinessException(FacilityErrorCode.FACILITY_IMAGE_DELETE_FAILED);
        }
        facility.removeImage(imageUrl);
    }

    public void deleteFacility(String managerId, String facilityId) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);

        boolean hasActiveSlots = facilitySlotRepository.existsByFacilityIdAndStatusIn(
                facilityId, List.of(SlotStatus.RESERVED, SlotStatus.PENDING)
        );
        if (hasActiveSlots) {
            throw new BusinessException(FacilityErrorCode.FACILITY_HAS_ACTIVE_RESERVATIONS);
        }

        facility.close();
    }

    @Transactional(readOnly = true)
    public List<FacilitySlotResponse> getSlotsByDate(String facilityId, LocalDate date) {
        getFacilityOrThrow(facilityId);
        return facilitySlotRepository.findAllByFacilityIdAndSlotDateOrderByStartTime(facilityId, date)
                .stream()
                .map(FacilitySlotResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacilityReservationOverviewResponse getReservations(
            String managerId,
            String facilityId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(FacilityErrorCode.FACILITY_RESERVATION_INVALID_DATE_RANGE);
        }

        List<FacilitySlot> slots = facilitySlotRepository
                .findAllByFacilityIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
                        facilityId,
                        fromDate,
                        toDate
                );
        if (slots.isEmpty()) {
            return emptyReservationOverview(facilityId, fromDate, toDate);
        }

        List<String> slotIds = slots.stream().map(FacilitySlot::getId).toList();
        Map<String, Reservation> reservationsBySlotId = reservationRepository
                .findAllByFacilitySlotIdIn(slotIds)
                .stream()
                .collect(Collectors.toMap(
                        Reservation::getFacilitySlotId,
                        Function.identity(),
                        this::latestReservation
                ));
        Map<String, Long> revenueBySlotId = paymentRepository
                .sumNetRevenueByFacilitySlotIds(
                        slotIds,
                        PaymentType.FACILITY,
                        List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED)
                )
                .stream()
                .collect(Collectors.toMap(
                        FacilityRevenueProjection::getFacilitySlotId,
                        projection -> projection.getNetRevenue() == null ? 0L : projection.getNetRevenue()
                ));

        List<FacilityReservationSlotResponse> slotResponses = slots.stream()
                .map(slot -> FacilityReservationSlotResponse.of(
                        slot,
                        reservationsBySlotId.get(slot.getId()),
                        revenueBySlotId.getOrDefault(slot.getId(), 0L)
                ))
                .toList();
        long reservedSlots = slots.stream()
                .filter(slot -> slot.getStatus() == SlotStatus.RESERVED)
                .count();
        long availableSlots = slots.stream()
                .filter(slot -> slot.getStatus() == SlotStatus.AVAILABLE)
                .count();
        long totalRevenue = slotResponses.stream()
                .mapToLong(FacilityReservationSlotResponse::revenue)
                .sum();

        return new FacilityReservationOverviewResponse(
                facilityId,
                fromDate,
                toDate,
                slots.size(),
                reservedSlots,
                availableSlots,
                totalRevenue,
                slotResponses
        );
    }

    private FacilityReservationOverviewResponse emptyReservationOverview(
            String facilityId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return new FacilityReservationOverviewResponse(
                facilityId,
                fromDate,
                toDate,
                0,
                0,
                0,
                0,
                List.of()
        );
    }

    private Reservation latestReservation(Reservation first, Reservation second) {
        return first.getReservedAt().isAfter(second.getReservedAt()) ? first : second;
    }

    @Transactional
    public List<FacilitySlotResponse> setupSlots(String managerId, String facilityId, SlotSetupRequest request) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);
        validateSlotSetupRequest(request);

        Set<LocalDate> existingDates = facilitySlotRepository
                .findExistingSlotDatesByFacilityIdAndDateBetweenAndStartTime(
                        facilityId, request.fromDate(), request.toDate(), request.startTime());

        List<FacilitySlot> slots = new ArrayList<>();
        LocalDate current = request.fromDate();

        while (!current.isAfter(request.toDate())) {
            if (existingDates.contains(current)) {
                throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_DUPLICATE);
            }

            int price = resolveSlotPrice(facility, request, current);
            slots.addAll(generateDailySlots(facility, current, request.startTime(), request.endTime(), price));
            current = current.plusDays(1);
        }

        facilitySlotRepository.saveAll(slots);
        return slots.stream().map(FacilitySlotResponse::from).toList();
    }

    private void validateSlotSetupRequest(SlotSetupRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_INVALID_TIME);
        }

        if (request.fromDate().isAfter(request.toDate())) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_INVALID_DATE_RANGE);
        }

        LocalDate maxSetupDate = LocalDate.now(TimeUtils.SERVICE_ZONE)
                .plusMonths(MAX_MONTHS_AHEAD)
                .with(TemporalAdjusters.lastDayOfMonth());
        if (request.toDate().isAfter(maxSetupDate)) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_TOO_FAR_AHEAD);
        }
    }

    private int resolveSlotPrice(Facility facility, SlotSetupRequest request, LocalDate date) {
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (isWeekend) {
            return request.weekendPrice() != null ? request.weekendPrice() : facility.getDefaultWeekendPrice();
        }
        return request.weekdayPrice() != null ? request.weekdayPrice() : facility.getDefaultWeekdayPrice();
    }

    @Transactional
    public FacilitySlotResponse updateSlot(String managerId, String facilityId,
                                           String slotId, SlotUpdateRequest request) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);

        FacilitySlot slot = facilitySlotRepository.findByIdAndFacilityId(slotId, facilityId)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_FOUND));

        if (!slot.isManagerEditable()) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_NOT_EDITABLE);
        }

        if (request.status() == SlotStatus.PENDING || request.status() == SlotStatus.RESERVED) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_INVALID_STATUS);
        }

        slot.update(request.price(), request.status());
        return FacilitySlotResponse.from(slot);
    }

    private List<FacilitySlot> generateDailySlots(Facility facility, LocalDate date,
                                                    LocalTime startTime, LocalTime endTime, int price) {
        List<FacilitySlot> slots = new ArrayList<>();
        LocalTime slotStart = startTime;

        while (!slotStart.plusMinutes(facility.getSlotDurationMinutes()).isAfter(endTime)) {
            LocalTime slotEnd = slotStart.plusMinutes(facility.getSlotDurationMinutes());
            slots.add(FacilitySlot.create(facility.getId(), date, slotStart, slotEnd, price));
            slotStart = slotEnd;
        }
        return slots;
    }

    private Facility getFacilityOrThrow(String facilityId) {
        return facilityRepository.findByIdAndStatusNot(facilityId, FacilityStatus.CLOSED)
                .orElseThrow(() -> new BusinessException(FacilityErrorCode.FACILITY_NOT_FOUND));
    }

    private void validateOwnership(Facility facility, String managerId) {
        if (!facility.isOwnedBy(managerId)) {
            throw new BusinessException(FacilityErrorCode.FACILITY_ACCESS_DENIED);
        }
    }
}
