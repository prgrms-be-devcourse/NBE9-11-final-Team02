package com.back.sportteam.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.mypage.dto.response.MatchPaymentResponse;
import com.back.sportteam.domain.mypage.exception.MypageErrorCode;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import com.back.sportteam.domain.settlement.entity.Settlement;
import com.back.sportteam.domain.settlement.entity.SettlementStatus;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyPagePaymentServiceTest {

    private static final String USER_ID = "user-001";
    private static final String HOST_ID = "host-001";
    private static final String RESERVATION_ID = "reservation-001";
    private static final String SLOT_ID = "slot-001";
    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, Month.JUNE, 1, 10, 0);
    private static final LocalDateTime REFUNDED_AT = LocalDateTime.of(2026, Month.JUNE, 10, 14, 0);

    @Mock private MatchRepository matchRepository;
    @Mock private MatchParticipantRepository matchParticipantRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private SettlementRepository settlementRepository;

    @InjectMocks
    private MyPagePaymentService myPagePaymentService;

    @Test
    void 존재하지_않는_경기_조회시_예외가_발생한다() {
        when(matchRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPagePaymentService.getMatchPayment(USER_ID, "invalid-id"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(MatchErrorCode.MATCH_NOT_FOUND));
    }

    @Test
    void 방장_시설결제_PAID_정산_HOLDING_정상_조회() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = paidFacilityPayment();
        Settlement settlement = holdingSettlement(matchId);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PAID))
                .thenReturn(Optional.of(payment));
        when(settlementRepository.findByMatchId(matchId)).thenReturn(Optional.of(settlement));

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(HOST_ID, matchId);

        assertThat(response.role()).isEqualTo("HOST");
        assertThat(response.hostDetail().facilityPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.hostDetail().settlementStatus()).isEqualTo(SettlementStatus.HOLDING);
        assertThat(response.hostDetail().refundedAt()).isNull();
        assertThat(response.participantDetail()).isNull();
    }

    @Test
    void 방장_경기_미완료시_정산_필드는_null이다() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = paidFacilityPayment();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PAID))
                .thenReturn(Optional.of(payment));
        when(settlementRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(HOST_ID, matchId);

        assertThat(response.hostDetail().settlementStatus()).isNull();
        assertThat(response.hostDetail().hostSettlementAmount()).isNull();
        assertThat(response.hostDetail().platformFee()).isNull();
    }

    @Test
    void 방장_정산_FAILED_상태는_노출되지_않는다() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = paidFacilityPayment();
        Settlement settlement = failedSettlement(matchId);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PAID))
                .thenReturn(Optional.of(payment));
        when(settlementRepository.findByMatchId(matchId)).thenReturn(Optional.of(settlement));

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(HOST_ID, matchId);

        assertThat(response.hostDetail().settlementStatus()).isNull();
        assertThat(response.hostDetail().hostSettlementAmount()).isNull();
        assertThat(response.hostDetail().platformFee()).isNull();
    }

    @Test
    void 방장_시설_결제가_환불_상태이면_환불일과_사유가_포함된다() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = refundedFacilityPayment();
        Refund refund = completedRefund();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PAID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.REFUNDED))
                .thenReturn(Optional.of(payment));
        when(refundRepository.findFirstByPayment_IdAndStatus(payment.getId(), RefundStatus.COMPLETED))
                .thenReturn(Optional.of(refund));
        when(settlementRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(HOST_ID, matchId);

        assertThat(response.hostDetail().facilityPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.hostDetail().refundedAt()).isEqualTo(REFUNDED_AT);
        assertThat(response.hostDetail().refundReason()).isEqualTo("경기 취소");
    }

    @Test
    void 방장_정산_SETTLED_상태_금액과_상태가_정상_반환된다() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = paidFacilityPayment();
        Settlement settlement = holdingSettlement(matchId);
        settlement.markSettled(LocalDateTime.of(2026, Month.JUNE, 20, 12, 0));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PAID))
                .thenReturn(Optional.of(payment));
        when(settlementRepository.findByMatchId(matchId)).thenReturn(Optional.of(settlement));

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(HOST_ID, matchId);

        assertThat(response.hostDetail().settlementStatus()).isEqualTo(SettlementStatus.SETTLED);
        assertThat(response.hostDetail().hostSettlementAmount()).isNotNull();
        assertThat(response.hostDetail().platformFee()).isNotNull();
    }

    @Test
    void 방장_시설_결제_PENDING_상태_정상_조회() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = Payment.create(null, HOST_ID, null, SLOT_ID, PaymentType.FACILITY, "uid-facility-pending", 100000);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PAID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.REFUNDED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PENDING))
                .thenReturn(Optional.of(payment));
        when(settlementRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(HOST_ID, matchId);

        assertThat(response.hostDetail().facilityPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.hostDetail().refundedAt()).isNull();
    }

    @Test
    void 방장_시설_결제_내역이_없으면_예외가_발생한다() {
        Match match = hostMatch();
        String matchId = match.getId();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation()));
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PAID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.REFUNDED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                HOST_ID, SLOT_ID, PaymentType.FACILITY, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPagePaymentService.getMatchPayment(HOST_ID, matchId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(MypageErrorCode.FACILITY_PAYMENT_NOT_FOUND));
    }

    @Test
    void 참가자가_결제완료_상태를_정상_조회한다() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = paidParticipationPayment(matchId);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId, USER_ID,
                List.of(MatchParticipantStatus.ACTIVE, MatchParticipantStatus.CANCELLED)))
                .thenReturn(true);
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                USER_ID, matchId, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Optional.of(payment));

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(USER_ID, matchId);

        assertThat(response.role()).isEqualTo("PARTICIPANT");
        assertThat(response.participantDetail().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.participantDetail().refundedAt()).isNull();
        assertThat(response.hostDetail()).isNull();
    }

    @Test
    void 참가자_참가비가_환불_상태이면_환불일과_사유가_포함된다() {
        Match match = hostMatch();
        String matchId = match.getId();
        Payment payment = refundedParticipationPayment(matchId);
        Refund refund = completedRefund();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId, USER_ID,
                List.of(MatchParticipantStatus.ACTIVE, MatchParticipantStatus.CANCELLED)))
                .thenReturn(true);
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                USER_ID, matchId, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                USER_ID, matchId, PaymentType.PARTICIPATION, PaymentStatus.REFUNDED))
                .thenReturn(Optional.of(payment));
        when(refundRepository.findFirstByPayment_IdAndStatus(payment.getId(), RefundStatus.COMPLETED))
                .thenReturn(Optional.of(refund));

        MatchPaymentResponse response = myPagePaymentService.getMatchPayment(USER_ID, matchId);

        assertThat(response.participantDetail().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.participantDetail().refundedAt()).isEqualTo(REFUNDED_AT);
        assertThat(response.participantDetail().refundReason()).isEqualTo("경기 취소");
    }

    @Test
    void 해당_경기_참여자가_아니면_접근_거부_예외가_발생한다() {
        Match match = hostMatch();
        String matchId = match.getId();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId, USER_ID,
                List.of(MatchParticipantStatus.ACTIVE, MatchParticipantStatus.CANCELLED)))
                .thenReturn(false);

        assertThatThrownBy(() -> myPagePaymentService.getMatchPayment(USER_ID, matchId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(CommonErrorCode.ACCESS_DENIED));
    }

    @Test
    void 참가자_결제_내역이_없으면_예외가_발생한다() {
        Match match = hostMatch();
        String matchId = match.getId();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId, USER_ID,
                List.of(MatchParticipantStatus.ACTIVE, MatchParticipantStatus.CANCELLED)))
                .thenReturn(true);
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                USER_ID, matchId, PaymentType.PARTICIPATION, PaymentStatus.PAID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                USER_ID, matchId, PaymentType.PARTICIPATION, PaymentStatus.REFUNDED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                USER_ID, matchId, PaymentType.PARTICIPATION, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPagePaymentService.getMatchPayment(USER_ID, matchId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(MypageErrorCode.PARTICIPATION_PAYMENT_NOT_FOUND));
    }

    private Match hostMatch() {
        return Match.create(MatchCreateCommand.builder()
                .reservationId(RESERVATION_ID)
                .hostId(HOST_ID)
                .title("테스트 경기")
                .sportType(SportType.FUTSAL)
                .capacity(10)
                .feePerPerson(15000)
                .matchDate(LocalDate.of(2026, Month.JUNE, 30))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .recruitDeadline(LocalDateTime.of(2026, Month.JUNE, 29, 10, 0))
                .cancelDeadline(LocalDateTime.of(2026, Month.JUNE, 28, 10, 0))
                .build());
    }

    private Reservation reservation() {
        return Reservation.pending(SLOT_ID, LocalDateTime.of(2026, Month.JUNE, 1, 9, 0));
    }

    private Payment paidFacilityPayment() {
        Payment p = Payment.create(null, HOST_ID, null, SLOT_ID, PaymentType.FACILITY, "uid-facility-1", 100000);
        p.complete("pg-tx-1", PAID_AT);
        return p;
    }

    private Payment refundedFacilityPayment() {
        Payment p = Payment.create(null, HOST_ID, null, SLOT_ID, PaymentType.FACILITY, "uid-facility-2", 100000);
        p.complete("pg-tx-2", PAID_AT);
        p.refund(100000, REFUNDED_AT);
        return p;
    }

    private Payment paidParticipationPayment(String matchId) {
        Payment p = Payment.create("participant-1", USER_ID, matchId, null, PaymentType.PARTICIPATION, "uid-part-1", 15000);
        p.complete("pg-tx-3", PAID_AT);
        return p;
    }

    private Payment refundedParticipationPayment(String matchId) {
        Payment p = Payment.create("participant-1", USER_ID, matchId, null, PaymentType.PARTICIPATION, "uid-part-2", 15000);
        p.complete("pg-tx-4", PAID_AT);
        p.refund(15000, REFUNDED_AT);
        return p;
    }

    private Settlement holdingSettlement(String matchId) {
        return Settlement.create(matchId, HOST_ID, SportType.FUTSAL, 60000, BigDecimal.valueOf(0.1));
    }

    private Settlement failedSettlement(String matchId) {
        Settlement s = Settlement.create(matchId, HOST_ID, SportType.FUTSAL, 60000, BigDecimal.valueOf(0.1));
        s.markFailed();
        return s;
    }

    private Refund completedRefund() {
        Refund refund = org.mockito.Mockito.mock(Refund.class);
        when(refund.getCompletedAt()).thenReturn(REFUNDED_AT);
        when(refund.getReason()).thenReturn("경기 취소");
        return refund;
    }
}
