package com.back.sportteam.domain.user.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("USER_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("USER_003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    ALREADY_WITHDRAWN("USER_004", "이미 탈퇴한 회원입니다.", HttpStatus.BAD_REQUEST),
    SPORT_STAT_NOT_FOUND("USER_005", "해당 종목에 대한 실력 점수가 등록되지 않았습니다.", HttpStatus.BAD_REQUEST),
    SPORT_STAT_ALREADY_EXISTS("USER_006", "이미 실력 점수가 등록된 종목입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}