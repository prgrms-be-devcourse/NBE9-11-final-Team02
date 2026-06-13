package com.back.sportteam.domain.facility.service;

import com.back.sportteam.domain.facility.dto.request.FacilityCreateRequest;
import com.back.sportteam.domain.facility.dto.request.FacilityUpdateRequest;
import com.back.sportteam.domain.facility.dto.request.SlotSetupRequest;
import com.back.sportteam.domain.facility.dto.response.FacilityResponse;
import com.back.sportteam.domain.facility.dto.response.FacilitySlotResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.entity.SlotStatus;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilitySlotRepository facilitySlotRepository;

    @Transactional
    public FacilityResponse createFacility(String managerId, FacilityCreateRequest request) {
        Facility facility = Facility.create(
                managerId,
                request.name(),
                request.address(),
                request.phone(),
                request.description(),
                request.slotDurationMinutes(),
                request.slotOpenAt(),
                request.sportTypes(),
                request.amenities(),
                request.imageUrls()
        );
        return FacilityResponse.from(facilityRepository.save(facility));
    }

    @Transactional
    public FacilityResponse updateFacility(String managerId, String facilityId, FacilityUpdateRequest request) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);

        facility.update(
                request.phone(),
                request.description(),
                request.slotDurationMinutes(),
                request.slotOpenAt(),
                request.sportTypes(),
                request.amenities(),
                request.imageUrls()
        );
        return FacilityResponse.from(facility);
    }

    @Transactional
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

    @Transactional
    public List<FacilitySlotResponse> setupSlots(String managerId, String facilityId, SlotSetupRequest request) {
        Facility facility = getFacilityOrThrow(facilityId);
        validateOwnership(facility, managerId);

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_INVALID_TIME);
        }

        List<FacilitySlot> slots = new ArrayList<>();
        LocalDate current = request.fromDate();

        while (!current.isAfter(request.toDate())) {
            if (facilitySlotRepository.existsByFacilityIdAndSlotDateAndStartTime(
                    facilityId, current, request.startTime())) {
                throw new BusinessException(FacilityErrorCode.FACILITY_SLOT_DUPLICATE);
            }

            boolean isWeekend = current.getDayOfWeek() == DayOfWeek.SATURDAY
                    || current.getDayOfWeek() == DayOfWeek.SUNDAY;
            int price = isWeekend ? request.weekendPrice() : request.weekdayPrice();

            slots.addAll(generateDailySlots(facility, current, request.startTime(), request.endTime(), price));
            current = current.plusDays(1);
        }

        facilitySlotRepository.saveAll(slots);
        return slots.stream().map(FacilitySlotResponse::from).toList();
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
