package com.back.sportteam.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.back.sportteam.domain.payment.dto.request.PaymentWebhookRequest;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEvent;
import com.back.sportteam.domain.payment.entity.PaymentWebhookEventType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.payment.repository.PaymentWebhookEventRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @InjectMocks
    private PaymentWebhookProcessor paymentWebhookProcessor;

    @Test
    void 결제_성공_웹훅이면_결제를_PAID로_변경하고_이벤트를_저장한다() {
        Payment payment = createPendingPayment();
        MatchParticipant participant = createPendingParticipant();
        PaymentWebhookRequest request = createRequest(
                "event-1",
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                "pg-transaction-1",
                10_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        when(matchParticipantRepository.findById("participant-id")).thenReturn(Optional.of(participant));

        paymentWebhookProcessor.process(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPgTransactionId()).isEqualTo("pg-transaction-1");
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.ACTIVE);
        assertThat(participant.getPaymentDeadline()).isNull();

        ArgumentCaptor<PaymentWebhookEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository).saveAndFlush(eventCaptor.capture());
        PaymentWebhookEvent event = eventCaptor.getValue();
        assertThat(event.getEventId()).isEqualTo("event-1");
        assertThat(event.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(event.getAmount()).isEqualTo(10_000);
    }

    @Test
    void 결제_실패_웹훅이면_결제를_FAILED로_변경하고_이벤트를_저장한다() {
        Payment payment = createPendingPayment();
        MatchParticipant participant = createPendingParticipant();
        participant.getMatch().increaseCurrentCount();
        PaymentWebhookRequest request = createRequest(
                "event-2",
                PaymentWebhookEventType.PAYMENT_FAILED,
                null,
                10_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        when(matchParticipantRepository.findById("participant-id")).thenReturn(Optional.of(participant));

        paymentWebhookProcessor.process(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPgTransactionId()).isNull();
        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(participant.getPaymentDeadline()).isNull();
        assertThat(participant.getMatch().getCurrentCount()).isEqualTo(1);
        verify(paymentWebhookEventRepository).saveAndFlush(any(PaymentWebhookEvent.class));
    }

    @Test
    void 방장_결제_실패_웹훅이면_매칭방도_취소한다() {
        Payment payment = createPendingPayment();
        MatchParticipant host = createPendingHost();
        PaymentWebhookRequest request = createRequest(
                "event-3",
                PaymentWebhookEventType.PAYMENT_FAILED,
                null,
                10_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));
        when(matchParticipantRepository.findById("participant-id")).thenReturn(Optional.of(host));

        paymentWebhookProcessor.process(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(host.getStatus()).isEqualTo(MatchParticipantStatus.CANCELLED);
        assertThat(host.getMatch().getCurrentCount()).isZero();
        assertThat(host.getMatch().getStatus()).isEqualTo(MatchStatus.CANCELLED);
        assertThat(host.getMatch().getCancelledAt()).isNotNull();
        verify(paymentWebhookEventRepository).saveAndFlush(any(PaymentWebhookEvent.class));
    }

    @Test
    void 이미_처리한_eventId면_결제와_이벤트를_다시_처리하지_않는다() {
        PaymentWebhookRequest request = createRequest(
                "event-1",
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                "pg-transaction-1",
                10_000
        );
        when(paymentWebhookEventRepository.existsByEventId("event-1")).thenReturn(true);

        paymentWebhookProcessor.process(request);

        verify(paymentRepository, never()).findByMerchantUidForUpdate(any());
        verify(paymentWebhookEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void 웹훅_금액이_주문_금액과_다르면_상태를_변경하지_않는다() {
        Payment payment = createPendingPayment();
        PaymentWebhookRequest request = createRequest(
                "event-1",
                PaymentWebhookEventType.PAYMENT_SUCCEEDED,
                "pg-transaction-1",
                9_000
        );
        when(paymentRepository.findByMerchantUidForUpdate("mid_12345")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentWebhookProcessor.process(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentWebhookEventRepository, never()).saveAndFlush(any());
    }

    private Payment createPendingPayment() {
        return Payment.create(
                "participant-id",
                "user-id",
                "match-id",
                null,
                PaymentType.PARTICIPATION,
                "mid_12345",
                10_000
        );
    }

    private MatchParticipant createPendingParticipant() {
        return MatchParticipant.participant(createMatch(), "user-id");
    }

    private MatchParticipant createPendingHost() {
        return MatchParticipant.host(createMatch(), "user-id");
    }

    private Match createMatch() {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId("reservation-id")
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .minParticipants(2)
                .maxParticipants(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.LEVEL_1)
                .maxSkillLevel(SkillLevel.LEVEL_5)
                .requiredGender(RequiredGender.ANY)
                .recruitDeadline(LocalDateTime.of(2099, Month.JUNE, 10, 10, 0))
                .cancelDeadline(LocalDateTime.of(2099, Month.JUNE, 12, 10, 0))
                .build());
        return match;
    }

    private PaymentWebhookRequest createRequest(
            String eventId,
            PaymentWebhookEventType eventType,
            String pgTransactionId,
            Integer amount
    ) {
        return new PaymentWebhookRequest(
                eventId,
                eventType,
                "mid_12345",
                pgTransactionId,
                amount
        );
    }
}
