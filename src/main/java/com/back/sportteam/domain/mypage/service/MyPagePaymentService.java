package com.back.sportteam.domain.mypage.service;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.exception.MatchErrorCode;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.mypage.dto.response.MatchPaymentResponse;
import com.back.sportteam.domain.mypage.exception.MyPageErrorCode;
import com.back.sportteam.domain.reservation.exception.ReservationErrorCode;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
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
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPagePaymentService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SettlementRepository settlementRepository;

    @Transactional(readOnly = true)
    public MatchPaymentResponse getMatchPayment(String userId, String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(MatchErrorCode.MATCH_NOT_FOUND));

        if (match.isHostedBy(userId)) {
            return buildHostResponse(userId, match);
        }

        boolean isParticipant = matchParticipantRepository.existsByMatchIdAndUserIdAndStatusIn(
                matchId, userId,
                List.of(MatchParticipantStatus.ACTIVE, MatchParticipantStatus.CANCELLED));
        if (!isParticipant) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }

        return buildParticipantResponse(userId, matchId);
    }

    private MatchPaymentResponse buildHostResponse(String userId, Match match) {
        Reservation reservation = reservationRepository.findById(match.getReservationId())
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        Payment facilityPayment = paymentRepository
                .findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                        userId, reservation.getFacilitySlotId(), PaymentType.FACILITY, PaymentStatus.PAID)
                .or(() -> paymentRepository.findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                        userId, reservation.getFacilitySlotId(), PaymentType.FACILITY, PaymentStatus.REFUNDED))
                .orElseGet(() -> paymentRepository
                        .findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
                                userId, reservation.getFacilitySlotId(), PaymentType.FACILITY, PaymentStatus.PENDING)
                        .orElseThrow(() -> new BusinessException(MyPageErrorCode.FACILITY_PAYMENT_NOT_FOUND)));

        LocalDateTime refundedAt = null;
        String refundReason = null;
        if (facilityPayment.getStatus() == PaymentStatus.REFUNDED) {
            Refund refund = refundRepository
                    .findFirstByPayment_IdAndStatus(facilityPayment.getId(), RefundStatus.COMPLETED)
                    .orElse(null);
            if (refund != null) {
                refundedAt = refund.getCompletedAt();
                refundReason = refund.getReason();
            }
        }

        Settlement settlement = settlementRepository.findByMatchId(match.getId()).orElse(null);
        boolean settlementVisible = settlement != null
                && settlement.getStatus() != SettlementStatus.FAILED;

        return MatchPaymentResponse.ofHost(
                facilityPayment.getAmount(),
                facilityPayment.getStatus(),
                facilityPayment.getPaidAt(),
                refundedAt,
                refundReason,
                settlementVisible ? settlement.getHostSettlementAmount() : null,
                settlementVisible ? settlement.getPlatformFee() : null,
                settlementVisible ? settlement.getStatus() : null
        );
    }

    private MatchPaymentResponse buildParticipantResponse(String userId, String matchId) {
        Payment payment = paymentRepository
                .findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                        userId, matchId, PaymentType.PARTICIPATION, PaymentStatus.PAID)
                .or(() -> paymentRepository.findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                        userId, matchId, PaymentType.PARTICIPATION, PaymentStatus.REFUNDED))
                .orElseGet(() -> paymentRepository
                        .findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
                                userId, matchId, PaymentType.PARTICIPATION, PaymentStatus.PENDING)
                        .orElseThrow(() -> new BusinessException(MyPageErrorCode.PARTICIPATION_PAYMENT_NOT_FOUND)));

        LocalDateTime refundedAt = null;
        String refundReason = null;
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            Refund refund = refundRepository
                    .findFirstByPayment_IdAndStatus(payment.getId(), RefundStatus.COMPLETED)
                    .orElse(null);
            if (refund != null) {
                refundedAt = refund.getCompletedAt();
                refundReason = refund.getReason();
            }
        }

        return MatchPaymentResponse.ofParticipant(
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                refundedAt,
                refundReason
        );
    }
}
