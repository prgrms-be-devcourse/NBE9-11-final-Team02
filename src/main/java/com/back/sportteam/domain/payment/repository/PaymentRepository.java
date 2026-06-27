package com.back.sportteam.domain.payment.repository;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByMerchantUid(String merchantUid);

    List<Payment> findAllByMatchIdAndStatus(String matchId, PaymentStatus status);

    List<Payment> findAllByFacilitySlotIdAndStatus(String facilitySlotId, PaymentStatus status);

    List<Payment> findAllByParticipantIdAndStatus(String participantId, PaymentStatus status);

    Optional<Payment> findFirstByUserIdAndMatchIdAndPaymentTypeAndStatus(
            String userId,
            String matchId,
            PaymentType paymentType,
            PaymentStatus status
    );

    Optional<Payment> findFirstByUserIdAndFacilitySlotIdAndPaymentTypeAndStatus(
            String userId,
            String facilitySlotId,
            PaymentType paymentType,
            PaymentStatus status
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.merchantUid = :merchantUid")
    Optional<Payment> findByMerchantUidForUpdate(@Param("merchantUid") String merchantUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment
            from Payment payment
            where payment.status = :status
              and payment.createdAt < :threshold
            order by payment.createdAt asc
            """)
    List<Payment> findStalePendingPaymentsForUpdate(
            @Param("status") PaymentStatus status,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable
    );

    @Query("""
            select sum(payment.amount)
            from Payment payment
            where payment.matchId = :matchId
              and payment.paymentType = :paymentType
              and payment.status = :status
            """)
    Long sumAmountByMatchId(
            @Param("matchId") String matchId,
            @Param("paymentType") PaymentType paymentType,
            @Param("status") PaymentStatus status
    );

    @Query("""
            select payment.matchId as matchId, sum(payment.amount) as total
            from Payment payment
            where payment.matchId in :matchIds
              and payment.paymentType = :paymentType
              and payment.status = :status
            group by payment.matchId
            """)
    List<Object[]> sumAmountByMatchIds(
            @Param("matchIds") List<String> matchIds,
            @Param("paymentType") PaymentType paymentType,
            @Param("status") PaymentStatus status
    );

    default Map<String, Long> sumAmountMapByMatchIds(
            List<String> matchIds,
            PaymentType paymentType,
            PaymentStatus status
    ) {
        return sumAmountByMatchIds(matchIds, paymentType, status).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    @Query("""
            select payment.facilitySlotId as facilitySlotId,
                   sum(payment.amount - payment.refundedAmount) as netRevenue
            from Payment payment
            where payment.facilitySlotId in :facilitySlotIds
              and payment.paymentType = :paymentType
              and payment.status in :statuses
            group by payment.facilitySlotId
            """)
    List<FacilityRevenueProjection> sumNetRevenueByFacilitySlotIds(
            @Param("facilitySlotIds") List<String> facilitySlotIds,
            @Param("paymentType") PaymentType paymentType,
            @Param("statuses") List<PaymentStatus> statuses
    );
}
