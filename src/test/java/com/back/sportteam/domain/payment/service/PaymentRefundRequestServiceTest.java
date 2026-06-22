package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRefundRequestServiceTest {

    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, Month.JUNE, 17, 12, 0);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private PaymentRefundRequestService paymentRefundRequestService;

    @Test
    void matchPaidParticipationPaymentIsQueuedForRefund() {
        Payment payment = createPaidPayment("participant-id", PaymentType.PARTICIPATION);
        when(paymentRepository.findAllByMatchIdAndStatus("match-id", PaymentStatus.PAID))
                .thenReturn(List.of(payment));
        when(reservationRepository.findById("reservation-id"))
                .thenReturn(Optional.of(reservation("slot-id")));
        when(paymentRepository.findAllByFacilitySlotIdAndStatus("slot-id", PaymentStatus.PAID))
                .thenReturn(List.of());

        paymentRefundRequestService.requestMatchRefunds(
                "match-id",
                "reservation-id",
                PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST,
                REQUESTED_AT
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Refund>> captor = ArgumentCaptor.forClass(List.class);
        verify(refundRepository).saveAll(captor.capture());

        Refund refund = captor.getValue().getFirst();
        assertThat(refund.getPayment()).isEqualTo(payment);
        assertThat(refund.getAmount()).isEqualTo(10_000);
        assertThat(refund.getReason()).isEqualTo(PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getRequestedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void duplicatePendingRefundIsNotCreated() {
        Payment payment = createPaidPayment("participant-id", PaymentType.PARTICIPATION);
        when(paymentRepository.findAllByMatchIdAndStatus("match-id", PaymentStatus.PAID))
                .thenReturn(List.of(payment));
        when(reservationRepository.findById("reservation-id"))
                .thenReturn(Optional.of(reservation("slot-id")));
        when(paymentRepository.findAllByFacilitySlotIdAndStatus("slot-id", PaymentStatus.PAID))
                .thenReturn(List.of());
        when(refundRepository.existsByPaymentIdAndStatus(
                payment.getId(),
                RefundStatus.PENDING
        ))
                .thenReturn(true);

        paymentRefundRequestService.requestMatchRefunds(
                "match-id",
                "reservation-id",
                PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST,
                REQUESTED_AT
        );

        verify(refundRepository).existsByPaymentIdAndStatus(payment.getId(), RefundStatus.PENDING);
        verify(refundRepository, never()).saveAll(any());
    }

    @Test
    void participantPaidPaymentIsQueuedForRefund() {
        Payment payment = createPaidPayment("participant-id", PaymentType.PARTICIPATION);
        when(paymentRepository.findAllByParticipantIdAndStatus("participant-id", PaymentStatus.PAID))
                .thenReturn(List.of(payment));

        paymentRefundRequestService.requestParticipantRefunds(
                "participant-id",
                PaymentRefundRequestService.MATCH_PARTICIPANT_LEFT,
                REQUESTED_AT
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Refund>> captor = ArgumentCaptor.forClass(List.class);
        verify(refundRepository).saveAll(captor.capture());

        Refund refund = captor.getValue().getFirst();
        assertThat(refund.getPayment()).isEqualTo(payment);
        assertThat(refund.getAmount()).isEqualTo(10_000);
        assertThat(refund.getReason()).isEqualTo(PaymentRefundRequestService.MATCH_PARTICIPANT_LEFT);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getRequestedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void matchPaidFacilityPaymentIsQueuedForRefundThroughReservationSlot() {
        Payment payment = createPaidFacilityPayment();
        when(paymentRepository.findAllByMatchIdAndStatus("match-id", PaymentStatus.PAID))
                .thenReturn(List.of());
        when(reservationRepository.findById("reservation-id"))
                .thenReturn(Optional.of(reservation("slot-id")));
        when(paymentRepository.findAllByFacilitySlotIdAndStatus("slot-id", PaymentStatus.PAID))
                .thenReturn(List.of(payment));

        paymentRefundRequestService.requestMatchRefunds(
                "match-id",
                "reservation-id",
                PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST,
                REQUESTED_AT
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Refund>> captor = ArgumentCaptor.forClass(List.class);
        verify(refundRepository).saveAll(captor.capture());

        Refund refund = captor.getValue().getFirst();
        assertThat(refund.getPayment()).isEqualTo(payment);
        assertThat(refund.getAmount()).isEqualTo(100_000);
        assertThat(refund.getReason()).isEqualTo(PaymentRefundRequestService.MATCH_CANCELLED_BY_HOST);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getRequestedAt()).isEqualTo(REQUESTED_AT);
    }

    private Reservation reservation(String facilitySlotId) {
        return Reservation.pending(facilitySlotId, REQUESTED_AT.minusMinutes(10));
    }

    private Payment createPaidPayment(String participantId, PaymentType paymentType) {
        Payment payment = Payment.create(
                participantId,
                "user-id",
                "match-id",
                null,
                paymentType,
                "mid_" + participantId,
                10_000
        );
        payment.complete("payment-key-" + participantId, REQUESTED_AT.minusMinutes(10));
        return payment;
    }

    private Payment createPaidFacilityPayment() {
        Payment payment = Payment.create(
                null,
                "host-id",
                null,
                "slot-id",
                PaymentType.FACILITY,
                "mid_facility",
                100_000
        );
        payment.complete("payment-key-facility", REQUESTED_AT.minusMinutes(10));
        return payment;
    }
}
