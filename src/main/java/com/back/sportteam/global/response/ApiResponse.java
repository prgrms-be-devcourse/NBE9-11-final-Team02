package com.back.sportteam.global.response;

import com.back.sportteam.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorInfo error;

    private ApiResponse(boolean success, T data, ErrorInfo error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null , null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message, String path) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorInfo(errorCode.getCode(), message, errorCode.getStatus().value(), path)
        );
    }

    @Getter
    public static class ErrorInfo {
        private final String code;
        private final String message;
        private final Integer status;
        private final String path;

        private ErrorInfo(String code, String message, Integer status, String path) {
            this.code = code;
            this.message = message;
            this.status = status;
            this.path = path;
        }
    }
}
