package com.back.sportteam.domain.payment.repository;

import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, String> {

    boolean existsByPaymentIdAndStatus(String paymentId, RefundStatus status);
}
