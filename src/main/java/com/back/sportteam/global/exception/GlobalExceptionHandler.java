package com.back.sportteam.global.exception;

import com.back.sportteam.global.exception.errorcode.ErrorCode;
import com.back.sportteam.global.response.ApiResponse;
import com.back.sportteam.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                errorCode.getStatus().value(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.failure(errorResponse));
    }
}
