package com.back.sportteam.domain.facility.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FacilityErrorCode implements ErrorCode {
    FACILITY_NOT_FOUND("FACILITY_001", "시설 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FACILITY_SLOT_NOT_FOUND("FACILITY_002", "시설 시간대 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FACILITY_ACCESS_DENIED("FACILITY_003", "해당 시설에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    FACILITY_HAS_ACTIVE_RESERVATIONS("FACILITY_004", "예약 확정된 슬롯이 있어 삭제할 수 없습니다.", HttpStatus.CONFLICT),
    FACILITY_SLOT_DUPLICATE("FACILITY_005", "같은 날짜와 시작시간에 슬롯을 중복 생성할 수 없습니다.", HttpStatus.CONFLICT),
    FACILITY_SLOT_INVALID_TIME("FACILITY_006", "영업 종료 시간은 시작 시간 이후여야 합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}