package com.back.sportteam.global.exception.errorcode;

import org.springframework.http.HttpStatus;

public enum PaymentErrorCode implements ErrorCode {

    INVALID_PAYMENT_REQUEST(
            "PAYMENT_001",
            "결제 요청 정보가 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST
    ),
    PAYMENT_AMOUNT_MISMATCH(
            "PAYMENT_002",
            "요청 금액이 서버의 결제 금액과 일치하지 않습니다.",
            HttpStatus.BAD_REQUEST
    ),
    PAYMENT_AMOUNT_SOURCE_UNAVAILABLE(
            "PAYMENT_003",
            "결제 금액 정보를 조회할 수 없습니다.",
            HttpStatus.SERVICE_UNAVAILABLE
    );

    private final String code;
    private final String message;
    private final HttpStatus status;

    PaymentErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }
}
