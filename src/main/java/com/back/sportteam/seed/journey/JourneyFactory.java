package com.back.sportteam.seed.journey;

import com.back.sportteam.domain.facility.entity.Amenity;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityDetails;
import com.back.sportteam.domain.facility.entity.FacilitySlot;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.facility.repository.FacilitySlotRepository;
import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.domain.reservation.entity.Reservation;
import com.back.sportteam.domain.reservation.repository.ReservationRepository;
import com.back.sportteam.domain.review.entity.ParticipantReview;
import com.back.sportteam.domain.review.repository.ParticipantReviewRepository;
import com.back.sportteam.domain.settlement.entity.Settlement;
import com.back.sportteam.domain.settlement.repository.SettlementRepository;
import com.back.sportteam.domain.user.entity.SelfReportedLevel;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.seed.support.SeedConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 매칭 한 건의 전체 생명주기(Facility→FacilitySlot→Reservation→Match→
 * MatchParticipant→Payment→Review→Settlement)를 하나의 트랜잭션으로 조립.
 *
 * 각 createJ* 메서드가 Journey 타입에 해당하는 최종 상태를 직접 만든다.
 * 스케줄러(완료 처리, 정산)가 실행되지 않아도 데모 데이터가 완결된다.
 *
 * J1  – RECRUITING  (미래, 일부 참가)
 * J2  – CONFIRMED   (미래, 방장 확정)
 * J3  – RECRUITING + FULL (미래, 정원 가득)
 * J4  – COMPLETED + SETTLED (과거)
 * J5  – COMPLETED, 정산 대기 (오늘, Settlement 없음)
 * J6/J7 – CANCELLED + 환불 (과거, 참가자 수만 다름)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"seed-fixture", "seed-bulk"})
public class JourneyFactory {

    private static final BigDecimal[] RATING_OPTIONS = {
        new BigDecimal("1.0"), new BigDecimal("1.5"), new BigDecimal("2.0"),
        new BigDecimal("2.5"), new BigDecimal("3.0"), new BigDecimal("3.5"),
        new BigDecimal("4.0"), new BigDecimal("4.5"), new BigDecimal("5.0")
    };

    private static final String[] DISTRICTS = {
        "강남구", "마포구", "송파구", "서초구", "용산구",
        "성동구", "영등포구", "강서구", "노원구", "중구"
    };

    private final FacilityRepository facilityRepository;
    private final FacilitySlotRepository facilitySlotRepository;
    private final ReservationRepository reservationRepository;
    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PaymentRepository paymentRepository;
    private final ParticipantReviewRepository participantReviewRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final UserSportStatRepository userSportStatRepository;

    // ─── J1: RECRUITING (미래, 일부 참가) ───────────────────────────
    @Transactional
    public void createJ1Recruiting(JourneyRequest req) {
        MatchBase base = buildMatchBase(req, false);
        addPaidParticipants(base, req);
        matchRepository.save(base.match());
        // status = RECRUITING 유지
    }

    // ─── J2: CONFIRMED (미래, 방장 확정) ────────────────────────────
    @Transactional
    public void createJ2Confirmed(JourneyRequest req) {
        MatchBase base = buildMatchBase(req, false);
        addPaidParticipants(base, req);

        LocalDateTime confirmedAt = req.matchDate().minusDays(1).atTime(12, 0);
        base.match().confirm(confirmedAt);
        matchRepository.save(base.match());
    }

    // ─── J3: RECRUITING + FULL (미래, 정원 가득) ─────────────────────
    // req.participants().size() + 1(host) == req.capacity() 를 호출측이 보장
    // 로직은 J1과 동일 — 상태 차이는 capacity가 가득 찬 것으로 호출측이 보장
    @Transactional
    public void createJ3Full(JourneyRequest req) {
        createJ1Recruiting(req);
    }

    // ─── J4: COMPLETED + SETTLED (과거) ─────────────────────────────
    @Transactional
    public void createJ4CompletedSettled(JourneyRequest req) {
        MatchBase base = buildMatchBase(req, true);
        addPaidParticipants(base, req);

        Match match = base.match();
        match.complete();
        matchRepository.save(match);

        List<User> allActive = activeUsers(req);
        generateReviews(match, allActive, req);

        int totalFee = req.participants().size() * req.feePerPerson();
        Settlement settlement = Settlement.create(
            match.getId(), req.host().getId(), req.sportType(),
            totalFee, SeedConstants.PLATFORM_FEE_RATE
        );
        settlementRepository.save(settlement);
    }

    // ─── J5: COMPLETED, 정산 대기 (오늘) ────────────────────────────
    @Transactional
    public void createJ5SettlementPending(JourneyRequest req) {
        // req.matchDate() == SeedConstants.today() 를 호출측이 보장
        MatchBase base = buildMatchBase(req, true);
        addPaidParticipants(base, req);

        Match match = base.match();
        match.complete();
        matchRepository.save(match);

        // Settlement 없음: 정산 배치는 matchDate < today 만 처리
    }

    // ─── J6/J7: CANCELLED + 환불 (과거) ────────────────────────────
    @Transactional
    public void createJCancelled(JourneyRequest req) {
        MatchBase base = buildMatchBase(req, false);

        // 참가자 추가 후 일괄 취소
        List<MatchParticipant> mps = new ArrayList<>();
        List<Payment> payments = new ArrayList<>();

        LocalDateTime paidAt = req.matchDate().minusDays(5).atTime(10, 0);
        for (User p : req.participants()) {
            MatchParticipant mp = MatchParticipant.participant(base.match(), p.getId());
            matchParticipantRepository.save(mp);

            Payment payment = Payment.create(
                mp.getId(), p.getId(), base.match().getId(), base.slot().getId(),
                PaymentType.PARTICIPATION,
                SeedConstants.MERCHANT_UID_PREFIX + UUID.randomUUID(),
                req.feePerPerson()
            );
            payment.complete("PG-SEED-" + UUID.randomUUID(), paidAt);
            mp.activate();
            base.match().increaseCurrentCount();

            paymentRepository.save(payment);
            matchParticipantRepository.save(mp);
            mps.add(mp);
            payments.add(payment);
        }

        LocalDateTime cancelledAt = req.matchDate().minusDays(2).atTime(9, 0);
        LocalDateTime refundedAt  = cancelledAt.plusHours(1);

        for (int i = 0; i < mps.size(); i++) {
            mps.get(i).cancel();
            payments.get(i).refund(req.feePerPerson(), refundedAt);
            base.match().decreaseCurrentCount();
        }

        base.reservation().cancel(cancelledAt);
        base.slot().release();
        base.match().cancel(cancelledAt);

        reservationRepository.save(base.reservation());
        facilitySlotRepository.save(base.slot());
        matchRepository.save(base.match());
    }

    // ─── Private: 공통 인프라 생성 ───────────────────────────────────

    private MatchBase buildMatchBase(JourneyRequest req, boolean isPast) {
        LocalTime endTime = req.startTime().plusHours(2);

        // 1. Facility
        Facility facility = Facility.create(
            req.host().getId(),
            req.facilityName(),
            generateAddress(req.rng()),
            FacilityDetails.builder()
                .phone(generatePhone(req.rng()))
                .description(req.facilityName() + " 체육시설")
                .capacity(req.capacity() + 4)
                .slotDurationMinutes(120)
                .defaultWeekdayPrice(req.feePerPerson() * req.capacity())
                .defaultWeekendPrice((int) (req.feePerPerson() * req.capacity() * 1.2))
                .slotOpenAt(null)
                .sportTypes(Set.of(req.sportType()))
                .amenities(Set.of(Amenity.PARKING))
                .imageUrls(List.of())
                .build()
        );
        facilityRepository.save(facility);

        // 2. FacilitySlot
        FacilitySlot slot = FacilitySlot.create(
            facility.getId(), req.matchDate(), req.startTime(), endTime,
            req.feePerPerson() * req.capacity()
        );
        slot.reserve(); // AVAILABLE → RESERVED (seeder 권한으로 직접 전이)
        facilitySlotRepository.save(slot);

        // 3. Reservation
        LocalDateTime reservedAt = req.matchDate().minusDays(7).atTime(10, 0);
        Reservation reservation = Reservation.pending(slot.getId(), reservedAt);
        reservation.confirm();
        if (isPast) {
            reservation.complete();
        }
        reservationRepository.save(reservation);

        // 4. Match
        LocalDateTime matchStartDt = req.matchDate().atTime(req.startTime());
        Match match = Match.create(MatchCreateCommand.builder()
            .reservationId(reservation.getId())
            .hostId(req.host().getId())
            .title(req.title())
            .sportType(req.sportType())
            .capacity(req.capacity())
            .feePerPerson(req.feePerPerson())
            .minSkillLevel(req.minLevel())
            .maxSkillLevel(req.maxLevel())
            .requiredGender(RequiredGender.ANY)
            .matchDate(req.matchDate())
            .startTime(req.startTime())
            .endTime(endTime)
            .recruitDeadline(matchStartDt.minusHours(1))
            .participantCancelDeadline(matchStartDt.minusDays(1))
            .hostCancelDeadline(matchStartDt.minusDays(3))
            .build());
        matchRepository.save(match);

        // 5. 방장 참가자 (ACTIVE, 결제 없음)
        MatchParticipant hostParticipant = MatchParticipant.host(match, req.host().getId());
        matchParticipantRepository.save(hostParticipant);

        return new MatchBase(facility, slot, reservation, match, hostParticipant);
    }

    private void addPaidParticipants(MatchBase base, JourneyRequest req) {
        LocalDateTime paidAt = req.matchDate().minusDays(3).atTime(14, 0);
        for (User p : req.participants()) {
            MatchParticipant mp = MatchParticipant.participant(base.match(), p.getId());
            matchParticipantRepository.save(mp);

            Payment payment = Payment.create(
                mp.getId(), p.getId(), base.match().getId(), base.slot().getId(),
                PaymentType.PARTICIPATION,
                SeedConstants.MERCHANT_UID_PREFIX + UUID.randomUUID(),
                req.feePerPerson()
            );
            payment.complete("PG-SEED-" + UUID.randomUUID(), paidAt);
            mp.activate();
            base.match().increaseCurrentCount();

            paymentRepository.save(payment);
            matchParticipantRepository.save(mp);
        }
    }

    private void generateReviews(Match match, List<User> allParticipants, JourneyRequest req) {
        for (User reviewer : allParticipants) {
            List<User> eligible = allParticipants.stream()
                .filter(u -> !u.getId().equals(reviewer.getId()))
                .toList();

            int count = req.rng().nextInt(3); // 0, 1, 2 리뷰 작성
            List<User> shuffled = new ArrayList<>(eligible);
            Collections.shuffle(shuffled, req.rng());

            for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
                User reviewee = shuffled.get(i);
                BigDecimal mannerRating = RATING_OPTIONS[req.rng().nextInt(RATING_OPTIONS.length)];
                BigDecimal skillRating  = RATING_OPTIONS[req.rng().nextInt(RATING_OPTIONS.length)];

                participantReviewRepository.save(
                    ParticipantReview.create(
                        match.getId(), reviewer.getId(), reviewee.getId(),
                        mannerRating, skillRating, null
                    )
                );

                // 다른 journey 트랜잭션이 이미 갱신했을 수 있으므로 DB에서 최신 값 로드
                User freshReviewee = userRepository.findById(reviewee.getId()).orElse(reviewee);
                freshReviewee.addMannerRating(mannerRating);
                userRepository.save(freshReviewee);

                updateSkillStat(freshReviewee, req.sportType(), skillRating);
            }
        }
    }

    private void updateSkillStat(User reviewee, com.back.sportteam.domain.match.entity.SportType sport, BigDecimal skillRating) {
        UserSportStat stat = userSportStatRepository
            .findByUser_IdAndSportType(reviewee.getId(), sport)
            .orElseGet(() -> {
                UserSportStat newStat = UserSportStat.create(reviewee, sport, SelfReportedLevel.INTERMEDIATE);
                return userSportStatRepository.save(newStat);
            });
        stat.addSkillRating(skillRating);
        userSportStatRepository.save(stat);
    }

    private List<User> activeUsers(JourneyRequest req) {
        List<User> all = new ArrayList<>();
        all.add(req.host());
        all.addAll(req.participants());
        return all;
    }

    private String generateAddress(Random rng) {
        String district = DISTRICTS[rng.nextInt(DISTRICTS.length)];
        int streetNum = rng.nextInt(100) + 1;
        int detailNum = rng.nextInt(50) + 1;
        return "서울 " + district + " " + streetNum + "번길 " + detailNum;
    }

    private String generatePhone(Random rng) {
        return String.format("02-%04d-%04d", rng.nextInt(9000) + 1000, rng.nextInt(9000) + 1000);
    }

    private record MatchBase(
        Facility facility,
        FacilitySlot slot,
        Reservation reservation,
        Match match,
        MatchParticipant hostParticipant
    ) {}
}
