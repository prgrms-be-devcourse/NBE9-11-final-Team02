package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentProvider;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.infra.redis.queue.WaitingQueueService;
import java.util.Optional;
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

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private WaitingQueueService waitingQueueService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void prepareCreatesParticipationPaymentWhenAmountMatches() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                null,
                10_000,
                PaymentType.PARTICIPATION
        );
        MatchParticipant participant = org.mockito.Mockito.mock(MatchParticipant.class);
        when(participant.getId()).thenReturn("participant-id");
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(10_000);
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                "user-id",
                "match-id",
                PaymentType.PARTICIPATION,
                PaymentStatus.PENDING
        )).thenReturn(Optional.empty());
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                "match-id",
                "user-id",
                MatchParticipantStatus.PAYMENT_PENDING
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
    void prepareThrowsWhenRequestedAmountDoesNotMatchServerAmount() {
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
    void prepareThrowsWhenServerAmountIsUnavailable() {
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
    void prepareThrowsWhenParticipantDoesNotExist() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                "match-id",
                null,
                10_000,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReader.getParticipationAmount("match-id")).thenReturn(10_000);
        when(paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                "user-id",
                "match-id",
                PaymentType.PARTICIPATION,
                PaymentStatus.PENDING
        )).thenReturn(Optional.empty());
        when(matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                "match-id",
                "user-id",
                MatchParticipantStatus.PAYMENT_PENDING
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.prepare("user-id", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_PARTICIPANT_NOT_FOUND);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void prepareCreatesFacilityPaymentWithQueueToken() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                null,
                "slot-id",
                100_000,
                PaymentType.FACILITY
        );
        when(paymentAmountReader.getFacilityAmount("slot-id")).thenReturn(100_000);
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                "user-id",
                "slot-id",
                PaymentType.FACILITY,
                PaymentStatus.PENDING
        )).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.prepare("user-id", "queue-token", request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        verify(waitingQueueService).consumeEnterableToken("queue-token", "slot-id", "user-id");
        Payment payment = paymentCaptor.getValue();
        assertThat(payment.getParticipantId()).isNull();
        assertThat(payment.getMatchId()).isNull();
        assertThat(payment.getFacilitySlotId()).isEqualTo("slot-id");
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.FACILITY);
        assertThat(payment.getAmount()).isEqualTo(100_000);
    }

    @Test
    void prepareReusesExistingPendingPayment() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                null,
                "slot-id",
                100_000,
                PaymentType.FACILITY
        );
        Payment existingPayment = Payment.create(
                null,
                "user-id",
                null,
                "slot-id",
                PaymentType.FACILITY,
                "mid_existing",
                100_000
        );
        when(paymentAmountReader.getFacilityAmount("slot-id")).thenReturn(100_000);
        when(paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                "user-id",
                "slot-id",
                PaymentType.FACILITY,
                PaymentStatus.PENDING
        )).thenReturn(Optional.of(existingPayment));

        PaymentPrepareResponse response = paymentService.prepare("user-id", "queue-token", request);

        assertThat(response.merchantUid()).isEqualTo("mid_existing");
        assertThat(response.amount()).isEqualTo(100_000);
        verify(waitingQueueService, never()).consumeEnterableToken(any(), any(), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void prepareThrowsWhenPaymentTargetIsInvalid() {
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
