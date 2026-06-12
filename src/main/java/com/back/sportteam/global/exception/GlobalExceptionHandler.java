package com.back.sportteam.global.exception;

import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String detail = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + getDefaultMessage(error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        CommonErrorCode.INVALID_INPUT,
                        buildValidationMessage(detail),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        String detail = e.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + getDefaultMessage(violation.getMessage()))
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        CommonErrorCode.INVALID_INPUT,
                        buildValidationMessage(detail),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        CommonErrorCode.INVALID_INPUT,
                        "요청 본문 형식이 올바르지 않습니다.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        String detail = "요청 파라미터 '" + e.getName() + "'의 값이 올바르지 않습니다.";

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT, detail, request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        String detail = "필수 요청 파라미터 '" + e.getParameterName() + "'이(가) 누락되었습니다.";

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT, detail, request.getRequestURI()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(
            MissingRequestHeaderException e,
            HttpServletRequest request
    ) {
        String detail = "필수 요청 헤더 '" + e.getHeaderName() + "'가 누락되었습니다.";

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT, detail, request.getRequestURI()));
    }

    // 인증/인가, DB 무결성 예외는 Security/JPA 도입 후 전용 핸들러로 분리합니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("Unexpected exception occurred", e);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                        request.getRequestURI()
                ));
    }

    private String buildValidationMessage(String detail) {
        if (detail == null || detail.isBlank()) {
            return "입력값 검증에 실패했습니다.";
        }
        return "입력값 검증에 실패했습니다. (" + detail + ")";
    }

    private String getDefaultMessage(String message) {
        if (message == null || message.isBlank()) {
            return CommonErrorCode.INVALID_INPUT.getMessage();
        }
        return message;
    }
}
