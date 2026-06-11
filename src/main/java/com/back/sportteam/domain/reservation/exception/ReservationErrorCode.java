package com.back.sportteam.domain.reservation.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {
    RESERVATION_NOT_FOUND("RESERVATION_001", "예약 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SLOT_NOT_AVAILABLE("RESERVATION_002", "예약할 수 없는 시간대입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
