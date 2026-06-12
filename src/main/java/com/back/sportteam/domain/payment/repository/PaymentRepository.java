package com.back.sportteam.domain.payment.repository;

import com.back.sportteam.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
