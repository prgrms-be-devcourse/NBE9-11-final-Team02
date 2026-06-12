package com.back.sportteam.domain.payment.service;

public interface PaymentAmountReader {

    Long getFacilityAmount(String matchId);

    Long getParticipationAmount(String matchId);
}
