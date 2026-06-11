package com.back.sportteam.domain.payment.service;

public interface PaymentAmountReader {

    Long getFacilityAmount(Long matchId);

    Long getParticipationAmount(Long matchId);
}
