package com.back.sportteam.domain.mypage.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MypageErrorCode implements ErrorCode {
    FACILITY_PAYMENT_NOT_FOUND("MYPAGE_001", "시설 결제 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PARTICIPATION_PAYMENT_NOT_FOUND("MYPAGE_002", "참가비 결제 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
