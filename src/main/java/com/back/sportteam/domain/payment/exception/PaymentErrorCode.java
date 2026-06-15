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
    REFUND_FAILED("PAYMENT_004", "환불 처리에 실패했습니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_AMOUNT_SOURCE_UNAVAILABLE(
            "PAYMENT_005",
            "결제 금액 정보를 조회할 수 없습니다.",
            HttpStatus.SERVICE_UNAVAILABLE
    ),
    PAYMENT_PARTICIPANT_NOT_FOUND(
            "PAYMENT_006",
            "결제 대상 참가자 정보를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND
    ),
    INVALID_PAYMENT_TARGET(
            "PAYMENT_007",
            "결제 유형과 결제 대상 정보가 일치하지 않습니다.",
            HttpStatus.BAD_REQUEST
    ),
    INVALID_WEBHOOK_SIGNATURE(
            "PAYMENT_008",
            "결제 웹훅 서명이 올바르지 않습니다.",
            HttpStatus.UNAUTHORIZED
    ),
    EXPIRED_WEBHOOK_REQUEST(
            "PAYMENT_009",
            "결제 웹훅 요청 유효 시간이 만료되었습니다.",
            HttpStatus.UNAUTHORIZED
    ),
    INVALID_WEBHOOK_PAYLOAD(
            "PAYMENT_010",
            "결제 웹훅 요청 형식이 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST
    ),
    INVALID_PAYMENT_STATUS_TRANSITION(
            "PAYMENT_011",
            "현재 결제 상태에서는 요청한 상태로 변경할 수 없습니다.",
            HttpStatus.CONFLICT
    ),
    PAYMENT_PROVIDER_VERIFICATION_FAILED(
            "PAYMENT_012",
            "PG사 결제 정보 검증에 실패했습니다.",
            HttpStatus.BAD_GATEWAY
    );

    private final String code;
    private final String message;
    private final HttpStatus status;
}
