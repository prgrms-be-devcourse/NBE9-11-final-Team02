package com.back.sportteam.domain.payment.service;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.exception.PaymentErrorCode;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String MERCHANT_UID_PREFIX = "mid_";

    private final PaymentRepository paymentRepository;
    private final PaymentAmountReader paymentAmountReader;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional
    public PaymentPrepareResponse prepare(String userId, PaymentPrepareRequest request) {
        Integer expectedAmount = getExpectedAmount(request);
        if (!expectedAmount.equals(request.amount())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        Payment payment = Payment.create(
                getParticipantId(userId, request),
                userId,
                request.matchId(),
                request.paymentType(),
                generateMerchantUid(),
                expectedAmount
        );

        return PaymentPrepareResponse.from(paymentRepository.save(payment));
    }

    private Integer getExpectedAmount(PaymentPrepareRequest request) {
        Integer expectedAmount = switch (request.paymentType()) {
            case FACILITY -> paymentAmountReader.getFacilityAmount(request.matchId());
            case PARTICIPATION -> paymentAmountReader.getParticipationAmount(request.matchId());
        };

        if (expectedAmount == null || expectedAmount <= 0) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_SOURCE_UNAVAILABLE);
        }

        return expectedAmount;
    }

    private String getParticipantId(String userId, PaymentPrepareRequest request) {
        if (request.paymentType() == PaymentType.FACILITY) {
            return null;
        }

        return matchParticipantRepository.findByMatchIdAndUserIdAndStatus(
                        request.matchId(),
                        userId,
                        MatchParticipantStatus.ACTIVE
                )
                .map(participant -> participant.getId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_PARTICIPANT_NOT_FOUND));
    }

    private String generateMerchantUid() {
        return MERCHANT_UID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
