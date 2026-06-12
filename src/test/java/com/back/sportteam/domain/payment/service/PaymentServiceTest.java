package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentProvider;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAmountReader paymentAmountReader;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void 결제_금액이_일치하면_결제_주문을_생성한다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                null,
                10_000,
                PaymentType.PARTICIPATION
        );
        MatchParticipant participant = org.mockito.Mockito.mock(MatchParticipant.class);
        when(participant.getId()).thenReturn("participant-id");
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(10_000);
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                "match-id",
                "user-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(Optional.of(participant));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPrepareResponse response = paymentService.prepare("user-id", request);

        assertThat(response.merchantUid()).startsWith("mid_");
        assertThat(response.amount()).isEqualTo(10_000);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment payment = paymentCaptor.getValue();
        assertThat(payment.getId()).hasSize(36);
        assertThat(payment.getParticipantId()).isEqualTo("participant-id");
        assertThat(payment.getUserId()).isEqualTo("user-id");
        assertThat(payment.getMatchId()).isEqualTo("match-id");
        assertThat(payment.getFacilitySlotId()).isNull();
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.PARTICIPATION);
        assertThat(payment.getRefundedAmount()).isZero();
        assertThat(payment.getPgProvider()).isEqualTo(PaymentProvider.TOSSPAYMENTS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaidAt()).isNull();
        assertThat(payment.getRefundedAt()).isNull();
    }

    @Test
    void 요청_금액이_서버_금액과_다르면_결제_주문을_생성하지_않는다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                null,
                1_000,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(10_000);

        assertThatThrownBy(() -> paymentService.prepare("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 서버_결제_금액을_조회할_수_없으면_결제_주문을_생성하지_않는다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                null,
                10_000,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(null);

        assertThatThrownBy(() -> paymentService.prepare("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_SOURCE_UNAVAILABLE);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 참가자_정보가_없으면_참가_결제_주문을_생성하지_않는다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                null,
                10_000,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(10_000);
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                "match-id",
                "user-id",
                MatchParticipantStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.prepare("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_PARTICIPANT_NOT_FOUND);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 시설_결제는_시설_슬롯_ID와_가격으로_주문을_생성한다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                null,
                "slot-id",
                100_000,
                PaymentType.FACILITY
        );
        when(paymentAmountReader.getFacilityAmount("slot-id")).thenReturn(100_000);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.prepare("user-id", request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment payment = paymentCaptor.getValue();
        assertThat(payment.getParticipantId()).isNull();
        assertThat(payment.getMatchId()).isNull();
        assertThat(payment.getFacilitySlotId()).isEqualTo("slot-id");
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.FACILITY);
        assertThat(payment.getAmount()).isEqualTo(100_000);
    }

    @Test
    void 결제_유형과_대상_ID_조합이_다르면_주문을_생성하지_않는다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                "slot-id",
                10_000,
                PaymentType.PARTICIPATION
        );

        assertThatThrownBy(() -> paymentService.prepare("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_TARGET);

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
