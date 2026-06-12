package com.back.sportteam.domain.payment.service;

public interface PaymentAmountReader {

    Integer getFacilityAmount(String matchId);

    Integer getParticipationAmount(String matchId);
}
