package com.back.sportteam.batch.settlement;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 정산 배치 쿼리 수/소요 시간 측정용 수동 테스트
 *
 * <p>실제 MySQL과 {@code rewriteBatchedStatements=true} 환경에서만 배치 INSERT 묶임을 확인할 수 있어 CI에서는 실행하지 않는다.
 * 로컬에서 측정하려면 MySQL을 띄운 뒤 환경변수를 지정해 실행
 *
 * <pre>
 * RUN_SETTLEMENT_PERF=true \
 * SETTLEMENT_PERF_DB_PASSWORD=&lt;비밀번호&gt; \
 * ./gradlew test --tests "*SettlementBatchPerformanceTest" --rerun-tasks --info
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_SETTLEMENT_PERF", matches = "true")
@TestPropertySource(properties = {
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.orm.jdbc.bind=TRACE",
        "spring.datasource.url=jdbc:mysql://localhost:3306/team02_dev?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&rewriteBatchedStatements=true",
        "spring.datasource.username=root",
        "spring.datasource.password=${SETTLEMENT_PERF_DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.properties.hibernate.jdbc.batch_size=50",
        "spring.jpa.properties.hibernate.order_inserts=true"
})
class SettlementBatchPerformanceTest {

    private static final LocalDate MATCH_DATE = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(2);
    private static final int MATCH_COUNT = 300;

    @Autowired
    private SettlementScheduler settlementScheduler;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager em;

    @Test
    void 정산_배치_쿼리수_측정() {
        // given: COMPLETED + feePerPerson > 0 + PAID 참가비가 있는 경기
        for (int i = 0; i < MATCH_COUNT; i++) {
            Match match = saveCompleted(MATCH_DATE);
            Payment payment = Payment.create(
                    UUID.randomUUID().toString(),
                    "user-" + i,
                    match.getId(),
                    null,
                    PaymentType.PARTICIPATION,
                    "merchant-" + i,
                    10_000
            );
            payment.complete("pg-tx-" + i, LocalDateTime.of(MATCH_DATE, LocalTime.of(12, 0)));
            paymentRepository.save(payment);
        }
        em.flush();
        em.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        long start = System.currentTimeMillis();
        settlementScheduler.settleCompletedMatches();
        em.flush();
        long elapsedMs = System.currentTimeMillis() - start;

        System.out.println("========== 정산 배치 성능 측정 ==========");
        System.out.println("경기 수(N)             : " + MATCH_COUNT);
        System.out.println("PreparedStatement 횟수 : " + stats.getPrepareStatementCount());
        System.out.println("엔티티 INSERT 수       : " + stats.getEntityInsertCount());
        System.out.println("엔티티 로드 수         : " + stats.getEntityLoadCount());
        System.out.println("소요 시간(ms)          : " + elapsedMs);
        System.out.println("=========================================");
    }

    private Match saveCompleted(LocalDate matchDate) {
        Match match = matchRepository.save(buildMatch(matchDate));
        match.confirm(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(1));
        match.complete();
        return matchRepository.save(match);
    }

    private Match buildMatch(LocalDate matchDate) {
        return Match.create(MatchCreateCommand.builder()
                .reservationId(UUID.randomUUID().toString())
                .hostId("host-id")
                .title("풋살 매칭")
                .sportType(SportType.FUTSAL)
                .capacity(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .matchDate(matchDate)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .recruitDeadline(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(2))
                .cancelDeadline(LocalDateTime.of(matchDate, LocalTime.of(10, 0)).minusDays(1))
                .build());
    }
}
