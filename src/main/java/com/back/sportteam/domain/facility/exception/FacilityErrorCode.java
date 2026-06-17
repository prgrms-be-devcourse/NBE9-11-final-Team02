package com.back.sportteam.domain.facility.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FacilityErrorCode implements ErrorCode {
    FACILITY_NOT_FOUND("FACILITY_001", "시설 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FACILITY_SLOT_NOT_FOUND("FACILITY_002", "해당 슬롯을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FACILITY_ACCESS_DENIED("FACILITY_003", "해당 시설에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    FACILITY_HAS_ACTIVE_RESERVATIONS("FACILITY_004", "예약 중이거나 확정된 슬롯이 있어 삭제할 수 없습니다.", HttpStatus.CONFLICT),
    FACILITY_SLOT_DUPLICATE("FACILITY_005", "같은 날짜와 시작시간에 슬롯을 중복 생성할 수 없습니다.", HttpStatus.CONFLICT),
    FACILITY_SLOT_INVALID_TIME("FACILITY_006", "영업 종료 시간은 시작 시간 이후여야 합니다.", HttpStatus.BAD_REQUEST),
    FACILITY_SLOT_NOT_EDITABLE("FACILITY_007", "예약 중이거나 확정된 슬롯은 수정할 수 없습니다.", HttpStatus.CONFLICT),
    FACILITY_SLOT_INVALID_STATUS("FACILITY_008", "슬롯 상태를 예약 대기 또는 예약 확정으로 직접 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
    FACILITY_SLOT_INVALID_DATE_RANGE("FACILITY_009", "시작 날짜는 종료 날짜보다 늦을 수 없습니다.", HttpStatus.BAD_REQUEST),
    FACILITY_SLOT_TOO_FAR_AHEAD("FACILITY_010", "슬롯은 오늘이 속한 달로부터 3개월 뒤 달의 마지막 날까지만 설정할 수 있습니다.", HttpStatus.BAD_REQUEST),
    FACILITY_SLOT_NOT_AVAILABLE("FACILITY_011", "예약 가능한 슬롯이 아닙니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
