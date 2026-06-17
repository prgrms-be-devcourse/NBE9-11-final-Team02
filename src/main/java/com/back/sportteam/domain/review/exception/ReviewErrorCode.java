package com.back.sportteam.domain.review.exception;

import com.back.sportteam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
    MATCH_NOT_COMPLETED("REVIEW_001", "종료된 경기에만 후기를 작성할 수 있습니다.", HttpStatus.BAD_REQUEST),
    NOT_A_PARTICIPANT("REVIEW_002", "실제 참가자만 후기를 작성할 수 있습니다.", HttpStatus.FORBIDDEN),
    ALREADY_REVIEWED_FACILITY("REVIEW_003", "이미 시설 후기를 작성했습니다.", HttpStatus.CONFLICT),
    CANNOT_REVIEW_SELF("REVIEW_004", "본인은 평가할 수 없습니다.", HttpStatus.BAD_REQUEST),
    REVIEWEE_NOT_PARTICIPANT("REVIEW_005", "평가 대상이 해당 경기의 참가자가 아닙니다.", HttpStatus.BAD_REQUEST),
    ALREADY_REVIEWED_PARTICIPANT("REVIEW_006", "이미 해당 참가자를 평가했습니다.", HttpStatus.CONFLICT),
    INVALID_RATING("REVIEW_007", "점수는 0.5 이상 5.0 이하, 0.5 단위로 입력해야 합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
