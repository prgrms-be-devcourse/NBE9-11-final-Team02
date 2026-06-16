package com.back.sportteam.domain.payment.repository;

import com.back.sportteam.domain.payment.entity.Refund;
import com.back.sportteam.domain.payment.entity.RefundStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefundRepository extends JpaRepository<Refund, String> {

    boolean existsByPaymentIdAndStatus(String paymentId, RefundStatus status);

    boolean existsByPaymentIdAndStatusIn(String paymentId, List<RefundStatus> statuses);

    @Query("""
            select refund.id
            from Refund refund
            where refund.status = :status
              and (refund.nextRetryAt is null or refund.nextRetryAt <= :now)
            order by refund.requestedAt asc
            """)
    List<String> findIdsByStatus(
            @Param("status") RefundStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Refund refund
            set refund.status = :pendingStatus,
                refund.nextRetryAt = :now
            where refund.status = :processingStatus
              and refund.lastAttemptedAt <= :cutoff
            """)
    int recoverStaleProcessingRefunds(
            @Param("processingStatus") RefundStatus processingStatus,
            @Param("pendingStatus") RefundStatus pendingStatus,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select refund from Refund refund join fetch refund.payment where refund.id = :refundId")
    Optional<Refund> findByIdForUpdate(@Param("refundId") String refundId);
}
