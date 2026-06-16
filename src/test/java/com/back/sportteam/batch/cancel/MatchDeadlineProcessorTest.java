package com.back.sportteam.batch.cancel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.RefundRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import com.back.sportteam.domain.reservation.service.ReservationSlotService;
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
class MatchDeadlineProcessorTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationSlotService reservationSlotService;

    @InjectMocks
    private MatchDeadlineProcessor matchDeadlineProcessor;

    @Test
    void 최소_인원을_충족한_마감_경기는_자동_확정한다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(1, processedAt.minusMinutes(1));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));

        matchDeadlineProcessor.process(match.getId(), processedAt);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(match.getConfirmedAt()).isEqualTo(processedAt);
        verify(reservationSlotService).confirmReservation(match.getReservationId());
        verify(paymentRepository, never()).findAllByMatchIdAndStatus(any(), any());
    }

    @Test
    void 최소_인원에_미달한_마감_경기는_취소하고_환불을_대기열에_등록한다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(2, processedAt.minusMinutes(1));
        MatchParticipant participant = mock(MatchParticipant.class);
        Payment payment = createPaidPayment(match.getId(), processedAt.minusMinutes(10));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatus(
                match.getId(),
                MatchParticipantStatus.ACTIVE
        )).thenReturn(List.of(participant));
        when(paymentRepository.findAllByMatchIdAndStatus(
                match.getId(),
                PaymentStatus.PAID
        )).thenReturn(List.of(payment));
        when(reservationRepository.findById(match.getReservationId())).thenReturn(Optional.empty());

        matchDeadlineProcessor.process(match.getId(), processedAt);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELLED);
        assertThat(match.getCancelledAt()).isEqualTo(processedAt);
        verify(reservationSlotService).cancelReservation(match.getReservationId(), processedAt);
        verify(participant).cancel();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Refund>> captor = ArgumentCaptor.forClass(List.class);
        verify(refundRepository).saveAll(captor.capture());
        Refund refund = captor.getValue().getFirst();
        assertThat(refund.getPayment()).isEqualTo(payment);
        assertThat(refund.getAmount()).isEqualTo(10_000);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getRequestedAt()).isEqualTo(processedAt);
    }

    @Test
    void 이미_대기_중인_환불이_있으면_중복_등록하지_않는다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(2, processedAt.minusMinutes(1));
        Payment payment = createPaidPayment(match.getId(), processedAt.minusMinutes(10));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatus(
                match.getId(),
                MatchParticipantStatus.ACTIVE
        )).thenReturn(List.of());
        when(paymentRepository.findAllByMatchIdAndStatus(
                match.getId(),
                PaymentStatus.PAID
        )).thenReturn(List.of(payment));
        when(refundRepository.existsByPaymentIdAndStatusIn(
                payment.getId(),
                List.of(RefundStatus.PENDING, RefundStatus.PROCESSING)
        )).thenReturn(true);

        matchDeadlineProcessor.process(match.getId(), processedAt);

        verify(refundRepository, never()).saveAll(any());
    }

    @Test
    void 최소_인원에_미달하면_방장_시설_결제도_환불_대기열에_등록한다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(2, processedAt.minusMinutes(1));
        Reservation reservation = mock(Reservation.class);
        Payment facilityPayment = createPaidFacilityPayment(processedAt.minusMinutes(10));
        when(reservation.getFacilitySlotId()).thenReturn("slot-id");
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
        when(matchParticipantRepository.findByMatchIdAndStatus(
                match.getId(),
                MatchParticipantStatus.ACTIVE
        )).thenReturn(List.of());
        when(paymentRepository.findAllByMatchIdAndStatus(
                match.getId(),
                PaymentStatus.PAID
        )).thenReturn(List.of());
        when(reservationRepository.findById(match.getReservationId())).thenReturn(Optional.of(reservation));
        when(paymentRepository.findAllByFacilitySlotIdAndStatus(
                "slot-id",
                PaymentStatus.PAID
        )).thenReturn(List.of(facilityPayment));

        matchDeadlineProcessor.process(match.getId(), processedAt);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Refund>> captor = ArgumentCaptor.forClass(List.class);
        verify(refundRepository).saveAll(captor.capture());
        Refund refund = captor.getValue().getFirst();
        assertThat(refund.getPayment()).isEqualTo(facilityPayment);
        assertThat(refund.getAmount()).isEqualTo(100_000);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
    }

    @Test
    void 이미_처리된_경기는_다시_처리하지_않는다() {
        LocalDateTime processedAt = LocalDateTime.of(2026, Month.JUNE, 15, 12, 0);
        Match match = createMatch(1, processedAt.minusMinutes(1));
        match.confirm(processedAt.minusSeconds(1));
        when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));

        matchDeadlineProcessor.process(match.getId(), processedAt);

        verify(matchParticipantRepository, never()).findByMatchIdAndStatus(any(), any());
        verify(paymentRepository, never()).findAllByMatchIdAndStatus(any(), any());
    }

    private Match createMatch(int minParticipants, LocalDateTime recruitDeadline) {
        return Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-" + minParticipants)
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .minParticipants(minParticipants)
                .maxParticipants(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(recruitDeadline)
                .cancelDeadline(recruitDeadline.plusHours(1))
                .build());
    }

    private Payment createPaidPayment(String matchId, LocalDateTime paidAt) {
        Payment payment = Payment.create(
                "participant-id",
                "user-id",
                matchId,
                null,
                PaymentType.PARTICIPATION,
                "mid_12345",
                10_000
        );
        payment.complete("payment-key", paidAt);
        return payment;
    }

    private Payment createPaidFacilityPayment(LocalDateTime paidAt) {
        Payment payment = Payment.create(
                null,
                "host-id",
                null,
                "slot-id",
                PaymentType.FACILITY,
                "mid_facility",
                100_000
        );
        payment.complete("facility-payment-key", paidAt);
        return payment;
    }
}
