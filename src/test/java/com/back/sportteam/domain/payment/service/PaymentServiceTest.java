package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.entity.Payment;
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

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAmountReader paymentAmountReader;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void 결제_금액이_일치하면_결제_주문을_생성한다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                10_000L,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(10_000L);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPrepareResponse response = paymentService.prepare(request);

        assertThat(response.merchantUid()).startsWith("mid_");
        assertThat(response.amount()).isEqualTo(10_000L);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getId()).hasSize(36);
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void 요청_금액이_서버_금액과_다르면_결제_주문을_생성하지_않는다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                1_000L,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(10_000L);

        assertThatThrownBy(() -> paymentService.prepare(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 서버_결제_금액을_조회할_수_없으면_결제_주문을_생성하지_않는다() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                10_000L,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(null);

        assertThatThrownBy(() -> paymentService.prepare(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_SOURCE_UNAVAILABLE);

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
