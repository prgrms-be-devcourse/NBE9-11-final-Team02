package com.back.sportteam.global.exception;

import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
import com.back.sportteam.global.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 비즈니스_예외를_ApiResponse_형식으로_변환한다() {
        MockHttpServletRequest request = request("/api/v1/matches/1");
        BusinessException exception = new BusinessException(MatchErrorCode.MATCH_NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception, request);

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "MATCH_001", "매칭방을 찾을 수 없습니다.", "/api/v1/matches/1");
    }

    @Test
    void RequestBody_검증_실패를_400_응답으로_변환한다() throws Exception {
        MockHttpServletRequest request = request("/api/v1/matches");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "request");
        bindingResult.addError(new FieldError("request", "title", "제목은 필수입니다."));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", SampleRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_002");
        assertThat(response.getBody().getError().getMessage()).contains("title: 제목은 필수입니다.");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/v1/matches");
    }

    @Test
    void PathVariable_또는_RequestParam_검증_실패를_400_응답으로_변환한다() {
        MockHttpServletRequest request = request("/api/v1/matches");
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("matchId");
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("0보다 커야 합니다.");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_002");
        assertThat(response.getBody().getError().getMessage()).contains("0보다 커야 합니다.");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/v1/matches");
    }

    @Test
    void Json_파싱_실패를_400_응답으로_변환한다() {
        MockHttpServletRequest request = request("/api/v1/matches");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "invalid json",
                mock(HttpInputMessage.class)
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleHttpMessageNotReadableException(exception, request);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "COMMON_002", "요청 본문 형식이 올바르지 않습니다.", "/api/v1/matches");
    }

    @Test
    void 타입_변환_실패를_400_응답으로_변환한다() {
        MockHttpServletRequest request = request("/api/v1/matches");
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "matchId",
                null,
                new NumberFormatException("For input string: abc")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodArgumentTypeMismatchException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_002");
        assertThat(response.getBody().getError().getMessage()).contains("matchId");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/v1/matches");
    }

    @Test
    void 필수_파라미터_누락을_400_응답으로_변환한다() {
        MockHttpServletRequest request = request("/api/v1/matches");
        MissingServletRequestParameterException exception = new MissingServletRequestParameterException("date", "String");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingServletRequestParameterException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("COMMON_002");
        assertThat(response.getBody().getError().getMessage()).contains("date");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/v1/matches");
    }

    @Test
    void 예상하지_못한_예외를_500_응답으로_변환한다() {
        MockHttpServletRequest request = request("/api/v1/matches");
        RuntimeException exception = new RuntimeException("sensitive message");

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpectedException(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "COMMON_001",
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                "/api/v1/matches"
        );
    }

    @Test
    void 검증_상세값이_비어있으면_기본_검증_메시지를_사용한다() throws Exception {
        MockHttpServletRequest request = request("/api/v1/matches");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "request");

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", SampleRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(exception, request);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "COMMON_002", "입력값 검증에 실패했습니다.", "/api/v1/matches");
    }

    @Test
    void 검증_필드_메시지가_비어있으면_기본_입력값_메시지를_사용한다() throws Exception {
        MockHttpServletRequest request = request("/api/v1/matches");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "request");
        bindingResult.addError(new FieldError("request", "title", null));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", SampleRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getMessage()).contains(CommonErrorCode.INVALID_INPUT.getMessage());
    }

    private MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    private void assertErrorResponse(
            ResponseEntity<ApiResponse<Void>> response,
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo(code);
        assertThat(response.getBody().getError().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getError().getStatus()).isEqualTo(status.value());
        assertThat(response.getBody().getError().getPath()).isEqualTo(path);
    }

    private void sampleMethod(SampleRequest request) {
    }

    private static class SampleRequest {
    }
}
