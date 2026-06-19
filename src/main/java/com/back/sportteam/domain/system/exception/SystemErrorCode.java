package com.back.sportteam.domain.system.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SystemErrorCode implements ErrorCode {
    QUEUE_TOKEN_INVALID("SYSTEM_001", "유효하지 않은 대기열 토큰입니다.", HttpStatus.UNAUTHORIZED),
    QUEUE_TOKEN_EXPIRED("SYSTEM_002", "만료된 대기열 토큰입니다.", HttpStatus.UNAUTHORIZED),
    QUEUE_TOKEN_REQUIRED("SYSTEM_003", "대기열 토큰이 필요합니다.", HttpStatus.BAD_REQUEST),
    QUEUE_NOT_ENTERABLE("SYSTEM_004", "아직 입장 가능한 순번이 아닙니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
