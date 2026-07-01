package com.back.sportteam.seed.bulk;

import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.SelfReportedLevel;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.seed.SeedTask;
import com.back.sportteam.seed.journey.JourneyFactory;
import com.back.sportteam.seed.journey.JourneyRequest;
import com.back.sportteam.seed.support.SeedConstants;
import com.back.sportteam.seed.support.SeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 볼륨 더미 데이터 생성. seed-bulk 프로파일에서만 활성화.
 *
 * Journey 분배 (기본 155경기):
 *   J1 RECRUITING        : 20
 *   J2 CONFIRMED         : 15
 *   J3 RECRUITING+FULL   : 10
 *   J4 COMPLETED+SETTLED : 85
 *   J5 SETTLEMENT PENDING:  5  (matchDate == today)
 *   J6 CANCELLED 환불    : 12
 *   J7 CANCELLED 조기취소:  8
 *   합계                 : 155
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("seed-bulk")
public class BulkSeeder implements SeedTask {

    private static final int[] CAPACITIES    = {5, 6, 8, 10, 12};
    private static final int[] FEES          = {5000, 8000, 10000, 15000, 20000};
    private static final LocalTime[] START_TIMES = {
        LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(14, 0),
        LocalTime.of(18, 0), LocalTime.of(19, 0), LocalTime.of(20, 0)
    };
    private static final SportType[] SPORTS  = SportType.values();
    private static final SkillLevel[] LEVELS = {
        SkillLevel.ANY, SkillLevel.LEVEL_1, SkillLevel.LEVEL_2,
        SkillLevel.LEVEL_3, SkillLevel.LEVEL_4
    };

    private final UserRepository userRepository;
    private final UserSportStatRepository userSportStatRepository;
    private final JourneyFactory journeyFactory;
    private final SeedProperties seedProperties;

    @Override
    public String markerType() {
        return SeedConstants.TYPE_BULK;
    }

    @Override
    public void seed() {
        Random rng = new Random(SeedConstants.RANDOM_SEED);
        int userCount    = seedProperties.bulk().users();
        int matchCount   = seedProperties.bulk().matches();

        log.info("[BulkSeeder] 유저 {}명 생성 시작", userCount);
        List<User> users = createBulkUsers(userCount, rng);

        log.info("[BulkSeeder] 매칭 {}건 생성 시작", matchCount);
        distributeJourneys(users, matchCount, rng);

        log.info("[BulkSeeder] 완료");
    }

    // ─── 유저 생성 ────────────────────────────────────────────────────

    public List<User> createBulkUsers(int count, Random rng) {
        String firstEmail = "bulk1" + SeedConstants.BULK_EMAIL_DOMAIN;
        if (userRepository.existsByEmail(firstEmail)) {
            // 마커(seed_run BULK)가 없는데 벌크 유저가 있다 = 이전 실행이 journey 도중 실패한 흔적.
            // 이 상태로 재실행하면 journey가 중복 생성되므로 진행하지 않고 중단한다.
            // 복구: 벌크 데이터(@seed.sportteam.local 유저 및 그 매칭)를 정리한 뒤 재실행할 것.
            throw new IllegalStateException(
                "벌크 유저가 이미 존재하나 BULK 마커가 없습니다. 이전 실행이 중단된 상태로 추정됩니다. "
                + "중복 방지를 위해 시딩을 중단합니다. 벌크 데이터를 정리한 뒤 다시 실행하세요.");
        }

        List<User> users = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            users.add(User.local(
                "bulk" + i + SeedConstants.BULK_EMAIL_DOMAIN,
                "선수" + i,
                "$2a$10$seedbulkpasswordhash00000000000000000000000000",
                UserRole.USER
            ));
        }
        List<User> saved = userRepository.saveAll(users);

        // 각 유저에 랜덤 스포츠 1~2개 stat 등록
        List<UserSportStat> stats = new ArrayList<>();
        for (User u : saved) {
            SportType primary = pick(SPORTS, rng);
            SelfReportedLevel level = pickLevel(rng);
            stats.add(UserSportStat.create(u, primary, level));

            if (rng.nextInt(3) > 0) { // 67% 확률로 2종목
                SportType secondary = pick(SPORTS, rng);
                if (secondary != primary) {
                    stats.add(UserSportStat.create(u, secondary, pickLevel(rng)));
                }
            }
        }
        userSportStatRepository.saveAll(stats);
        return saved;
    }

    // ─── Journey 분배 ─────────────────────────────────────────────────

    private void distributeJourneys(List<User> users, int total, Random rng) {
        // 비율 기반으로 각 타입별 건수 산출 (합계 = total)
        int j1 = scale(total, 20);
        int j2 = scale(total, 15);
        int j3 = scale(total, 10);
        int j5 = scale(total, 5);
        int j6 = scale(total, 12);
        int j7 = scale(total, 8);
        int j4 = total - j1 - j2 - j3 - j5 - j6 - j7; // 나머지 전부 J4

        LocalDate today = SeedConstants.today();
        LocalDate start = SeedConstants.periodStart();

        log.info("[BulkSeeder] J1={} J2={} J3={} J4={} J5={} J6={} J7={}", j1, j2, j3, j4, j5, j6, j7);

        for (int i = 0; i < j1; i++) {
            journeyFactory.createJ1Recruiting(buildReq(users, today.plusDays(randBetween(1, 30, rng)), true, rng));
        }
        for (int i = 0; i < j2; i++) {
            // CONFIRMED는 마감 시 정원이 가득 찬 경기만 도달 가능(MatchDeadlineProcessor) → full
            journeyFactory.createJ2Confirmed(buildReq(users, today.plusDays(randBetween(1, 30, rng)), rng, true));
        }
        for (int i = 0; i < j3; i++) {
            journeyFactory.createJ3Full(buildReq(users, today.plusDays(randBetween(1, 30, rng)), rng, true));
        }
        for (int i = 0; i < j4; i++) {
            // COMPLETED는 CONFIRMED(정원 충족)를 거친 경기만 도달 가능 → full
            journeyFactory.createJ4CompletedSettled(buildReq(users, randomPast(start, today.minusDays(1), rng), rng, true));
        }
        for (int i = 0; i < j5; i++) {
            journeyFactory.createJ5SettlementPending(buildReqToday(users, today, rng));
        }
        for (int i = 0; i < j6; i++) {
            journeyFactory.createJCancelled(buildReq(users, randomPast(start, today.minusDays(3), rng), false, rng));
        }
        for (int i = 0; i < j7; i++) {
            // J7: 조기 취소 → 참가자 수가 정원의 절반 미만
            journeyFactory.createJCancelled(buildReqLowParticipants(users, randomPast(start, today.minusDays(3), rng), rng));
        }
    }

    // ─── Request 빌더 ────────────────────────────────────────────────

    /** 일반 요청: 정원의 절반 ~ 전체 참가자 */
    private JourneyRequest buildReq(List<User> users, LocalDate matchDate, boolean isFuture, Random rng) {
        int capacity    = pick(CAPACITIES, rng);
        int fee         = pick(FEES, rng);
        LocalTime start = pick(START_TIMES, rng);
        SportType sport = pick(SPORTS, rng);
        SkillLevel min  = pick(LEVELS, rng);
        SkillLevel max  = adjustMax(min, rng);

        User host = pickUser(users, rng);
        int participantCount = isFuture
            ? randBetween(1, capacity - 1, rng)           // 미래: 1 ~ capacity-1
            : randBetween(2, capacity - 1, rng);           // 과거: 최소 2명(리뷰 의미있게)
        List<User> participants = pickParticipants(users, host, participantCount, rng);

        return new JourneyRequest(
            host, participants, matchDate, start, sport, capacity, fee,
            min, max, generateTitle(sport, rng), generateFacilityName(sport, rng), rng
        );
    }

    /** J3용: 정원 가득(참가자 수 = capacity - 1) */
    private JourneyRequest buildReq(List<User> users, LocalDate matchDate, Random rng, boolean full) {
        int capacity    = pick(CAPACITIES, rng);
        int fee         = pick(FEES, rng);
        LocalTime start = pick(START_TIMES, rng);
        SportType sport = pick(SPORTS, rng);
        SkillLevel min  = pick(LEVELS, rng);
        SkillLevel max  = adjustMax(min, rng);

        User host = pickUser(users, rng);
        int participantCount = full ? capacity - 1 : randBetween(1, capacity - 1, rng);
        List<User> participants = pickParticipants(users, host, participantCount, rng);

        return new JourneyRequest(
            host, participants, matchDate, start, sport, capacity, fee,
            min, max, generateTitle(sport, rng), generateFacilityName(sport, rng), rng
        );
    }

    /** J5 전용: matchDate = today, startTime = 오전 */
    private JourneyRequest buildReqToday(List<User> users, LocalDate today, Random rng) {
        int capacity    = pick(CAPACITIES, rng);
        int fee         = pick(FEES, rng);
        LocalTime start = LocalTime.of(randBetween(7, 10, rng), 0); // 이미 종료된 오전 시간
        SportType sport = pick(SPORTS, rng);
        SkillLevel min  = pick(LEVELS, rng);
        SkillLevel max  = adjustMax(min, rng);

        User host = pickUser(users, rng);
        // J5는 COMPLETED(정산 대기) → 정원이 가득 찼던 경기여야 함
        int participantCount = capacity - 1;
        List<User> participants = pickParticipants(users, host, participantCount, rng);

        return new JourneyRequest(
            host, participants, today, start, sport, capacity, fee,
            min, max, generateTitle(sport, rng), generateFacilityName(sport, rng), rng
        );
    }

    /** J7 전용: 참가자 수 정원의 절반 미만 */
    private JourneyRequest buildReqLowParticipants(List<User> users, LocalDate matchDate, Random rng) {
        int capacity    = pick(CAPACITIES, rng);
        int fee         = pick(FEES, rng);
        LocalTime start = pick(START_TIMES, rng);
        SportType sport = pick(SPORTS, rng);
        SkillLevel min  = pick(LEVELS, rng);
        SkillLevel max  = adjustMax(min, rng);

        User host = pickUser(users, rng);
        int maxLow = Math.max(1, capacity / 2 - 1);
        int participantCount = randBetween(0, maxLow, rng);
        List<User> participants = pickParticipants(users, host, participantCount, rng);

        return new JourneyRequest(
            host, participants, matchDate, start, sport, capacity, fee,
            min, max, generateTitle(sport, rng), generateFacilityName(sport, rng), rng
        );
    }

    // ─── Utilities ───────────────────────────────────────────────────

    private static <T> T pick(T[] arr, Random rng) {
        return arr[rng.nextInt(arr.length)];
    }

    private static int pick(int[] arr, Random rng) {
        return arr[rng.nextInt(arr.length)];
    }

    private static SelfReportedLevel pickLevel(Random rng) {
        SelfReportedLevel[] levels = SelfReportedLevel.values();
        return levels[rng.nextInt(levels.length)];
    }

    private static User pickUser(List<User> users, Random rng) {
        return users.get(rng.nextInt(users.size()));
    }

    private static List<User> pickParticipants(List<User> users, User exclude, int count, Random rng) {
        List<User> pool = new ArrayList<>(users);
        pool.remove(exclude);
        Collections.shuffle(pool, rng);
        return new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
    }

    private static SkillLevel adjustMax(SkillLevel min, Random rng) {
        SkillLevel[] levels = SkillLevel.values();
        int minIdx = min.ordinal();
        int maxIdx = minIdx + rng.nextInt(levels.length - minIdx);
        return levels[maxIdx];
    }

    private static int randBetween(int lo, int hi, Random rng) {
        if (lo >= hi) return lo;
        return lo + rng.nextInt(hi - lo + 1);
    }

    private static LocalDate randomPast(LocalDate from, LocalDate to, Random rng) {
        long days = from.until(to, java.time.temporal.ChronoUnit.DAYS);
        if (days <= 0) return from;
        return from.plusDays(rng.nextInt((int) days + 1));
    }

    private static int scale(int total, int part) {
        return (int) Math.round(total * part / 155.0);
    }

    private static final String[] SPORT_NOUNS = {
        "매칭", "경기", "모임", "풋살", "농구", "테니스", "배드민턴"
    };

    private String generateTitle(SportType sport, Random rng) {
        String[] adj = {"즐거운", "활기찬", "열정적인", "친선", "실력향상"};
        return pick(adj, rng) + " " + sport.name().toLowerCase() + " " + pick(SPORT_NOUNS, rng);
    }

    private String generateFacilityName(SportType sport, Random rng) {
        String[] names = {"스포츠센터", "풋살장", "체육관", "스포츠파크", "클럽"};
        return sport.name() + " " + pick(names, rng) + (rng.nextInt(50) + 1) + "호점";
    }
}
