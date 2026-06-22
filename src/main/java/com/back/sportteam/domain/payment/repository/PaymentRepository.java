package com.back.sportteam.domain.payment.repository;

import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentStatus;
import com.back.sportteam.domain.payment.entity.PaymentType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
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
}
