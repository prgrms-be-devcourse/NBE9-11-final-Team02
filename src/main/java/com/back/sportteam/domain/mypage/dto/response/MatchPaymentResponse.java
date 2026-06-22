package com.back.sportteam.domain.mypage.dto.response;

import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.settlement.entity.SettlementStatus;
import java.time.LocalDateTime;

public record MatchPaymentResponse(
        String role,
        HostDetail hostDetail,
        ParticipantDetail participantDetail
) {
    public record HostDetail(
            int facilityPaymentAmount,
            PaymentStatus facilityPaymentStatus,
            LocalDateTime paidAt,
            LocalDateTime refundedAt,
            String refundReason,
            Integer hostSettlementAmount,
            Integer platformFee,
            SettlementStatus settlementStatus
    ) {}

    public record ParticipantDetail(
            int amount,
            PaymentStatus status,
            LocalDateTime paidAt,
            LocalDateTime refundedAt,
            String refundReason
    ) {}

    public static MatchPaymentResponse ofHost(
            int facilityPaymentAmount,
            PaymentStatus facilityPaymentStatus,
            LocalDateTime paidAt,
            LocalDateTime refundedAt,
            String refundReason,
            Integer hostSettlementAmount,
            Integer platformFee,
            SettlementStatus settlementStatus
    ) {
        return new MatchPaymentResponse(
                "HOST",
                new HostDetail(facilityPaymentAmount, facilityPaymentStatus, paidAt,
                        refundedAt, refundReason, hostSettlementAmount, platformFee, settlementStatus),
                null
        );
    }

    public static MatchPaymentResponse ofParticipant(
            int amount,
            PaymentStatus status,
            LocalDateTime paidAt,
            LocalDateTime refundedAt,
            String refundReason
    ) {
        return new MatchPaymentResponse(
                "PARTICIPANT",
                null,
                new ParticipantDetail(amount, status, paidAt, refundedAt, refundReason)
        );
    }
}
