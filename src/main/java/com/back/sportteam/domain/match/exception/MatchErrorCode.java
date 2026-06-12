package com.back.sportteam.domain.match.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchErrorCode implements ErrorCode {
    MATCH_NOT_FOUND("MATCH_001", "매칭방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MATCH_FULL("MATCH_002", "매칭방 정원이 가득 찼습니다.", HttpStatus.CONFLICT),
    ALREADY_PARTICIPATED("MATCH_003", "이미 참가한 매칭입니다.", HttpStatus.CONFLICT),
    NOT_MATCH_OWNER("MATCH_004", "매칭방장만 처리할 수 있습니다.", HttpStatus.FORBIDDEN),
    SLOT_ALREADY_RESERVED("MATCH_005", "이미 예약된 시간대입니다.", HttpStatus.CONFLICT),
    INVALID_PARTICIPANT_RANGE("MATCH_006", "최소 인원은 최대 인원보다 클 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_SKILL_LEVEL_RANGE("MATCH_007", "실력 레벨 범위가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
