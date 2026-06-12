package com.back.sportteam.domain.payment.service;

public interface PaymentAmountReader {

    Integer getFacilityAmount(String facilitySlotId);

    Integer getParticipationAmount(String matchId);
}
