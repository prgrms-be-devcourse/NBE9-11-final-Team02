package com.back.sportteam.seed.fixture;

import com.back.sportteam.domain.auth.security.PasswordHasher;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 픽스처(시나리오) 더미 데이터 생성. seed-fixture 프로파일에서만 활성화.
 *
 * 생성 내용
 *   · 픽스처 유저 6명 (박매니저/김방장/이중수/최초보/정성장/한고수)
 *   · 방장 전용 Journey 7개 (축구J1×1, 풋살J2×1, J4×2, J5×1, J6×1, 농구J7×1)
 *   · 김방장 참가 Journey 11개 (벌크 유저 방장 — 풋살4+축구5+배드민턴2)
 *   · 데모 매칭 4개
 *
 * 김방장 종목별 경험치 (마이페이지 시연용):
 *   풋살 8회 (방장 4회 / 참가 4회)
 *   축구 7회 (방장 2회 / 참가 5회)
 *   배드민턴 3회 (참가 3회 — 이중수 방장 1 + 벌크 방장 2)
 *
 * 박매니저(MANAGER)는 어떤 경기에도 참가하지 않음 — 시설 관리 역할 전담.
 * seed-fixture 단독 실행 시 벌크 유저가 없으므로 로그 경고 후 픽스처 유저로 대체.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("seed-fixture")
public class FixtureSeeder implements SeedTask {

    /** 팀원 시연용 공통 비밀번호. PBKDF2 해시는 유저 생성 시 PasswordHasher로 실시간 생성. */
    private static final String FIXTURE_PASSWORD = "test1234!!";

    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;
    private final UserSportStatRepository userSportStatRepository;
    private final JourneyFactory journeyFactory;

    @Override
    public String markerType() {
        return SeedConstants.TYPE_FIXTURE;
    }

    @Override
    public void seed() {
        guardAgainstPartialRun();

        Random rng = new Random(SeedConstants.RANDOM_SEED);

        log.info("[FixtureSeeder] 픽스처 유저 6명 생성");
        Fu u = createFixtureUsers();

        List<User> bulk = loadBulkUsers();
        if (bulk.isEmpty()) {
            log.warn("[FixtureSeeder] 벌크 유저 없음 — seed-bulk 없이 실행됨. 픽스처 유저로 대체.");
        } else {
            log.info("[FixtureSeeder] 벌크 유저 {}명 로드됨", bulk.size());
        }

        log.info("[FixtureSeeder] 방장 전용 Journey 생성");
        createHostJourneys(u, bulk, rng);

        log.info("[FixtureSeeder] 김방장 참가 Journey 생성 (벌크 방장)");
        createHostParticipationJourneys(u, bulk, rng);

        log.info("[FixtureSeeder] 데모 매칭 4개 생성");
        createDemoMatches(u, bulk, rng);
    }

    // ─── 부분 실패 가드 ──────────────────────────────────────────────

    private void guardAgainstPartialRun() {
        if (userRepository.existsByEmail("fixture.host@seed.sportteam.local")) {
            throw new IllegalStateException(
                "픽스처 유저가 이미 존재하나 FIXTURE 마커가 없습니다. 이전 실행이 중단된 상태로 추정됩니다. "
                + "중복 방지를 위해 시딩을 중단합니다. 픽스처 데이터를 정리한 뒤 다시 실행하세요.");
        }
    }

    // ─── 유저 생성 ────────────────────────────────────────────────────

    private Fu createFixtureUsers() {
        User manager  = upsertUser("fixture.manager@seed.sportteam.local",  "박매니저",  UserRole.MANAGER);
        User host     = upsertUser("fixture.host@seed.sportteam.local",      "김방장",    UserRole.USER);
        User middle   = upsertUser("fixture.middle@seed.sportteam.local",    "이중수",    UserRole.USER);
        User beginner = upsertUser("fixture.beginner@seed.sportteam.local",  "최초보",    UserRole.USER);
        User rising   = upsertUser("fixture.rising@seed.sportteam.local",    "정성장",    UserRole.USER);
        User declined = upsertUser("fixture.declined@seed.sportteam.local",  "한고수",    UserRole.USER);

        addStatIfAbsent(manager,  SportType.FUTSAL,      SelfReportedLevel.INTERMEDIATE);
        addStatIfAbsent(host,     SportType.FUTSAL,      SelfReportedLevel.ADVANCED);
        addStatIfAbsent(host,     SportType.SOCCER,      SelfReportedLevel.INTERMEDIATE);
        addStatIfAbsent(host,     SportType.BADMINTON,   SelfReportedLevel.INTERMEDIATE);
        addStatIfAbsent(middle,   SportType.FUTSAL,      SelfReportedLevel.INTERMEDIATE);
        addStatIfAbsent(middle,   SportType.BASKETBALL,  SelfReportedLevel.INTERMEDIATE);
        addStatIfAbsent(beginner, SportType.FUTSAL,      SelfReportedLevel.BEGINNER);
        addStatIfAbsent(rising,   SportType.FUTSAL,      SelfReportedLevel.BEGINNER);
        addStatIfAbsent(declined, SportType.FUTSAL,      SelfReportedLevel.ADVANCED);

        return new Fu(manager, host, middle, beginner, rising, declined);
    }

    private User upsertUser(String email, String nickname, UserRole role) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(
                User.local(email, nickname, passwordHasher.hash(FIXTURE_PASSWORD), role)
            ));
    }

    private void addStatIfAbsent(User user, SportType sport, SelfReportedLevel level) {
        if (userSportStatRepository.findByUser_IdAndSportType(user.getId(), sport).isEmpty()) {
            userSportStatRepository.save(UserSportStat.create(user, sport, level));
        }
    }

    // BulkSeeder가 먼저 실행되므로 DB에서 조회 가능. 없으면 빈 리스트 반환.
    private List<User> loadBulkUsers() {
        List<User> result = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            userRepository.findByEmail("bulk" + i + SeedConstants.BULK_EMAIL_DOMAIN)
                .ifPresent(result::add);
        }
        return result;
    }

    /** 벌크 유저 없을 경우 픽스처 유저로 fallback */
    private User b(List<User> bulk, int idx, Fu u) {
        if (bulk.isEmpty()) return u.middle;
        return bulk.get(idx % bulk.size());
    }

    // ─── 방장 전용 Journey ────────────────────────────────────────────
    // 김방장 방장: 풋살 4회(J2×1, J4×2, J5×1, J6×1) + 축구 2회(J1×1, J4×1) + 농구 1회(J7×1)

    private void createHostJourneys(Fu u, List<User> bulk, Random rng) {
        LocalDate today = SeedConstants.today();

        // 축구 방장 1: J1 모집중 (미래) — 5v5 정원 10, 6명
        journeyFactory.createJ1Recruiting(new JourneyRequest(u.host,
            List.of(u.rising, b(bulk, 0, u), b(bulk, 1, u), b(bulk, 2, u), b(bulk, 3, u)),
            today.plusDays(14), LocalTime.of(10, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "주말 오전 축구", "강남 축구장", rng));

        // 풋살 방장 1: J2 확정 (미래) — 3v3 정원 6, 4명 확정
        journeyFactory.createJ2Confirmed(new JourneyRequest(u.host,
            List.of(u.middle, u.beginner, u.rising),
            today.plusDays(3), LocalTime.of(20, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "방장 확정 풋살", "서초 풋살센터", rng));

        // 풋살 방장 2: J4 완료+정산 2주전 — 5v5 정원 10, 8명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            List.of(u.middle, u.beginner, u.rising, b(bulk, 4, u), b(bulk, 5, u), b(bulk, 6, u), b(bulk, 7, u)),
            today.minusDays(14), LocalTime.of(19, 0), SportType.FUTSAL, 10, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "지난 풋살 (2주 전)", "마포 풋살센터", rng));

        // 축구 방장 2: J4 완료+정산 1달전 — 5v5 정원 10, 9명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            List.of(u.declined, u.rising, u.middle,
                    b(bulk, 8, u), b(bulk, 9, u), b(bulk, 10, u), b(bulk, 11, u), b(bulk, 12, u)),
            today.minusDays(30), LocalTime.of(20, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_5, "지난 축구 (1달 전)", "용산 축구장", rng));

        // 풋살 방장 3: J5 오늘 정산대기 — 3v3 정원 6, 5명
        journeyFactory.createJ5SettlementPending(new JourneyRequest(u.host,
            List.of(u.middle, u.beginner, b(bulk, 13, u), b(bulk, 14, u)),
            today, LocalTime.of(9, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "오늘 풋살 (정산 대기)", "노원 풋살파크", rng));

        // 풋살 방장 4: J6 취소+환불 — 정원 6, 3명으로 취소
        journeyFactory.createJCancelled(new JourneyRequest(u.host,
            List.of(u.rising, u.declined),
            today.minusDays(21), LocalTime.of(19, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "취소된 풋살", "중구 풋살장", rng));

        // 농구 방장 1: J7 조기취소 — 5v5 정원 10, 조기취소라 2명만
        journeyFactory.createJCancelled(new JourneyRequest(u.host,
            List.of(u.beginner),
            today.minusDays(28), LocalTime.of(14, 0), SportType.BASKETBALL, 10, 15000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "조기취소 농구", "영등포 체육관", rng));
    }

    // ─── 김방장 참가 Journey (벌크 유저 방장) ────────────────────────
    // 풋살 참가 4회 + 축구 참가 5회 + 배드민턴 참가 2회

    private void createHostParticipationJourneys(Fu u, List<User> bulk, Random rng) {
        LocalDate today = SeedConstants.today();

        // 풋살 참가 1: J1 모집중 (미래)
        journeyFactory.createJ1Recruiting(new JourneyRequest(b(bulk, 0, u),
            List.of(u.host, u.middle, b(bulk, 1, u), b(bulk, 2, u), b(bulk, 3, u)),
            today.plusDays(8), LocalTime.of(19, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "풋살 모집중 (bulk)", "풋살장 A", rng));

        // 풋살 참가 2: J3 정원가득 (미래) — 5v5 정원 10
        journeyFactory.createJ3Full(new JourneyRequest(b(bulk, 4, u),
            List.of(u.host, u.middle, u.rising, u.declined,
                    b(bulk, 5, u), b(bulk, 6, u), b(bulk, 7, u), b(bulk, 8, u), b(bulk, 9, u)),
            today.plusDays(12), LocalTime.of(18, 0), SportType.FUTSAL, 10, 12000,
            SkillLevel.LEVEL_2, SkillLevel.LEVEL_4, "정원가득 풋살 (bulk)", "풋살장 B", rng));

        // 풋살 참가 3: J4 완료+정산
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(b(bulk, 10, u),
            List.of(u.host, u.beginner, b(bulk, 11, u), b(bulk, 12, u), b(bulk, 13, u)),
            today.minusDays(10), LocalTime.of(19, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 풋살 참가 (bulk)", "풋살장 C", rng));

        // 풋살 참가 4: J4 완료+정산
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(b(bulk, 14, u),
            List.of(u.host, u.middle, u.declined, b(bulk, 15, u), b(bulk, 16, u)),
            today.minusDays(50), LocalTime.of(20, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.ANY, "완료 풋살 참가 2 (bulk)", "풋살장 D", rng));

        // 축구 참가 1: J1 모집중 (미래)
        journeyFactory.createJ1Recruiting(new JourneyRequest(b(bulk, 0, u),
            List.of(u.host, u.rising, b(bulk, 1, u), b(bulk, 2, u), b(bulk, 3, u)),
            today.plusDays(20), LocalTime.of(10, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "축구 모집중 (bulk)", "축구장 A", rng));

        // 축구 참가 2: J2 확정 (미래)
        journeyFactory.createJ2Confirmed(new JourneyRequest(b(bulk, 4, u),
            List.of(u.host, u.middle, b(bulk, 5, u), b(bulk, 6, u), b(bulk, 7, u)),
            today.plusDays(6), LocalTime.of(14, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "축구 확정 (bulk)", "축구장 B", rng));

        // 축구 참가 3: J4 완료+정산
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(b(bulk, 8, u),
            List.of(u.host, u.declined, b(bulk, 9, u), b(bulk, 10, u), b(bulk, 11, u),
                    b(bulk, 12, u), b(bulk, 13, u)),
            today.minusDays(7), LocalTime.of(19, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 축구 참가 (bulk)", "축구장 C", rng));

        // 축구 참가 4: J4 완료+정산
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(b(bulk, 14, u),
            List.of(u.host, u.rising, b(bulk, 15, u), b(bulk, 16, u), b(bulk, 17, u),
                    b(bulk, 18, u), b(bulk, 19, u)),
            today.minusDays(60), LocalTime.of(20, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.ANY, "완료 축구 참가 2 (bulk)", "축구장 D", rng));

        // 축구 참가 5: J6 취소
        journeyFactory.createJCancelled(new JourneyRequest(b(bulk, 3, u),
            List.of(u.host, b(bulk, 4, u), b(bulk, 5, u)),
            today.minusDays(15), LocalTime.of(18, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "취소 축구 참가 (bulk)", "축구장 E", rng));

        // 배드민턴 참가 1: J4 완료+정산
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(b(bulk, 6, u),
            List.of(u.host, u.middle, b(bulk, 7, u), b(bulk, 8, u)),
            today.minusDays(20), LocalTime.of(15, 0), SportType.BADMINTON, 6, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 배드민턴 참가 (bulk)", "배드민턴장 A", rng));

        // 배드민턴 참가 2: J1 모집중 (미래)
        journeyFactory.createJ1Recruiting(new JourneyRequest(b(bulk, 9, u),
            List.of(u.host, b(bulk, 10, u), b(bulk, 11, u)),
            today.plusDays(9), LocalTime.of(16, 0), SportType.BADMINTON, 6, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "배드민턴 모집중 (bulk)", "배드민턴장 B", rng));
    }

    // ─── 데모 매칭 4개 ───────────────────────────────────────────────

    private void createDemoMatches(Fu u, List<User> bulk, Random rng) {
        LocalDate today = SeedConstants.today();

        // 강남 퇴근후풋살: 이중수 방장, 정원 8, 6명 모집중
        journeyFactory.createJ1Recruiting(new JourneyRequest(u.middle,
            List.of(u.beginner, u.rising, b(bulk, 0, u), b(bulk, 1, u), b(bulk, 2, u)),
            today.plusDays(5), LocalTime.of(19, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "강남 퇴근후풋살", "강남 퇴근후풋살 센터", rng));

        // 마감임박 인기풋살: 한고수 방장, 정원 6, 5명(마감임박)
        journeyFactory.createJ1Recruiting(new JourneyRequest(u.declined,
            List.of(u.middle, u.beginner, u.rising, b(bulk, 3, u)),
            today.plusDays(2), LocalTime.of(20, 0), SportType.FUTSAL, 6, 12000,
            SkillLevel.LEVEL_2, SkillLevel.LEVEL_5, "마감임박 인기풋살", "서초 프리미엄 풋살", rng));

        // 모집완료 농구: 정성장 방장, 5v5 정원 10, 7명 확정
        journeyFactory.createJ2Confirmed(new JourneyRequest(u.rising,
            List.of(u.host, u.middle, u.declined, b(bulk, 4, u), b(bulk, 5, u), b(bulk, 6, u)),
            today.plusDays(4), LocalTime.of(18, 0), SportType.BASKETBALL, 10, 15000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "모집완료 농구 경기", "송파 실내 농구장", rng));

        // 정원가득찬 배드민턴: 이중수 방장, 3v3 정원 6, 6명 가득 — 김방장 배드민턴 3회 중 1회
        journeyFactory.createJ3Full(new JourneyRequest(u.middle,
            List.of(u.host, u.beginner, u.rising, u.declined, b(bulk, 7, u)),
            today.plusDays(6), LocalTime.of(14, 0), SportType.BADMINTON, 6, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "정원가득찬 배드민턴", "강남 배드민턴클럽", rng));
    }

    /** 픽스처 유저 6명 묶음 레코드. */
    private record Fu(User manager, User host, User middle, User beginner, User rising, User declined) {}
}
