package com.back.sportteam.global.exception;

import com.back.sportteam.domain.auth.exception.AuthErrorCode;
import com.back.sportteam.domain.facility.exception.FacilityErrorCode;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.reservation.exception.ReservationErrorCode;
import com.back.sportteam.domain.settlement.exception.SettlementErrorCode;
import com.back.sportteam.domain.system.exception.SystemErrorCode;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
import com.back.sportteam.global.response.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeConventionTest {

    @Test
    void 모든_에러코드는_필수값을_가진다() {
        allErrorCodes().forEach(errorCode -> {
            assertThat(errorCode.getCode()).isNotBlank();
            assertThat(errorCode.getMessage()).isNotBlank();
            assertThat(errorCode.getStatus()).isNotNull();
        });
    }

    @Test
    void 에러코드는_중복되지_않는다() {
        List<String> codes = allErrorCodes()
                .map(ErrorCode::getCode)
                .toList();

        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    void 에러코드는_도메인_prefix를_사용한다() {
        assertPrefix("COMMON", CommonErrorCode.values());
        assertPrefix("AUTH", AuthErrorCode.values());
        assertPrefix("MATCH", MatchErrorCode.values());
        assertPrefix("PAYMENT", PaymentErrorCode.values());
        assertPrefix("RESERVATION", ReservationErrorCode.values());
        assertPrefix("USER", UserErrorCode.values());
        assertPrefix("FACILITY", FacilityErrorCode.values());
        assertPrefix("SETTLEMENT", SettlementErrorCode.values());
        assertPrefix("SYSTEM", SystemErrorCode.values());
    }

    @Test
    void 비즈니스_예외는_에러코드의_상태와_메시지를_사용한다() {
        BusinessException exception = new BusinessException(MatchErrorCode.MATCH_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(MatchErrorCode.MATCH_NOT_FOUND);
        assertThat(exception.getStatus()).isEqualTo(MatchErrorCode.MATCH_NOT_FOUND.getStatus());
        assertThat(exception.getMessage()).isEqualTo(MatchErrorCode.MATCH_NOT_FOUND.getMessage());
    }

    @Test
    void 비즈니스_예외는_상세_메시지를_재정의할_수_있다() {
        BusinessException exception = new BusinessException(
                MatchErrorCode.MATCH_FULL,
                "현재 참가 인원이 가득 차 더 이상 참가할 수 없습니다."
        );

        assertThat(exception.getErrorCode()).isEqualTo(MatchErrorCode.MATCH_FULL);
        assertThat(exception.getStatus()).isEqualTo(MatchErrorCode.MATCH_FULL.getStatus());
        assertThat(exception.getMessage()).isEqualTo("현재 참가 인원이 가득 차 더 이상 참가할 수 없습니다.");
    }

    @Test
    void 성공_응답은_data를_담는다() {
        ApiResponse<String> response = ApiResponse.ok("ok");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("ok");
        assertThat(response.getError()).isNull();
    }

    @Test
    void 데이터가_없는_성공_응답을_만든다() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    void 실패_응답은_에러정보와_요청경로를_담는다() {
        ApiResponse<Void> response = ApiResponse.error(
                MatchErrorCode.MATCH_FULL,
                MatchErrorCode.MATCH_FULL.getMessage(),
                "/api/v1/matches/1/participants"
        );

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("MATCH_002");
        assertThat(response.getError().getMessage()).isEqualTo("매칭방 정원이 가득 찼습니다.");
        assertThat(response.getError().getStatus()).isEqualTo(409);
        assertThat(response.getError().getPath()).isEqualTo("/api/v1/matches/1/participants");
    }

    private Stream<ErrorCode> allErrorCodes() {
        return Stream.of(
                        Arrays.stream(CommonErrorCode.values()),
                        Arrays.stream(AuthErrorCode.values()),
                        Arrays.stream(MatchErrorCode.values()),
                        Arrays.stream(PaymentErrorCode.values()),
                        Arrays.stream(ReservationErrorCode.values()),
                        Arrays.stream(UserErrorCode.values()),
                        Arrays.stream(FacilityErrorCode.values()),
                        Arrays.stream(SettlementErrorCode.values()),
                        Arrays.stream(SystemErrorCode.values())
                )
                .flatMap(Function.identity());
    }

    private void assertPrefix(String prefix, ErrorCode[] errorCodes) {
        assertThat(errorCodes)
                .extracting(ErrorCode::getCode)
                .allMatch(code -> code.startsWith(prefix + "_"));
    }
}
