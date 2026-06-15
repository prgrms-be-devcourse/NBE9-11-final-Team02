package com.back.sportteam.domain.payment.repository;

import com.back.sportteam.domain.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, String> {

    boolean existsByEventId(String eventId);
}
