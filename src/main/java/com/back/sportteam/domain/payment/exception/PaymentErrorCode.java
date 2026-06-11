package com.back.sportteam.domain.payment.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_FOUND("PAYMENT_001", "결제 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PAYMENT_AMOUNT_MISMATCH("PAYMENT_002", "결제 금액이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED("PAYMENT_003", "결제 처리에 실패했습니다.", HttpStatus.BAD_REQUEST),
    REFUND_FAILED("PAYMENT_004", "환불 처리에 실패했습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
