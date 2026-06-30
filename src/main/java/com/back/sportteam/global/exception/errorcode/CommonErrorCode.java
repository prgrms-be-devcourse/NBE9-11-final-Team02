package com.back.sportteam.global.exception.errorcode;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    // === 공통 (1000~1999) ===
    INTERNAL_SERVER_ERROR("COMMON_001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("COMMON_002", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED("COMMON_003", "해당 리소스에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    LOCK_ACQUISITION_FAILED("COMMON_004", "잠시 후 다시 시도해주세요.", HttpStatus.CONFLICT),
    METHOD_NOT_ALLOWED("COMMON_005", "지원하지 않는 요청 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
