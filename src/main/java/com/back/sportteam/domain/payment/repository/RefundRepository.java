package com.back.sportteam.domain.payment.repository;

import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, String> {

    boolean existsByPaymentIdAndStatus(String paymentId, RefundStatus status);

    @Query("""
            select refund.id
            from Refund refund
            where refund.status = :status
            order by refund.requestedAt asc
            """)
    List<String> findIdsByStatus(
            @Param("status") RefundStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select refund from Refund refund join fetch refund.payment where refund.id = :refundId")
    Optional<Refund> findByIdForUpdate(@Param("refundId") String refundId);
}
