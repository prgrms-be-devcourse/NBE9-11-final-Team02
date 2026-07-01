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
 * 픽스처(시나리오) 더미 데이터 생성. seed-fixture 프로파일에서만 활성화.
 *
 * ── 김방장(u.host) 마이페이지 종목별 통계 목표 ──────────────────────
 * MyPageRecordRepository 는 mp.status=ACTIVE AND match.status=COMPLETED 인
 * 경기만 종목별 횟수로 집계한다. (미래/취소 경기는 통계에 잡히지 않음)
 * 따라서 아래는 전부 COMPLETED(J4) 또는 정산대기(J5, 내부적으로 COMPLETED) 경기다.
 *
 *   풋살 8회   = 방장 4회(J4×3 + J5×1) + 참가 4회(J4×4)
 *   축구 7회   = 방장 3회(J4×3)        + 참가 4회(J4×4)
 *   배드민턴 3회= 참가 3회(J4×3)
 *
 * 위와 별개로, /matches 목록 데모용 미래·취소 경기를 추가 생성한다.
 * 이 경기들은 종목별 통계에 잡히지 않으므로 위 8/7/3 수치에 영향을 주지 않는다.
 * (일부는 김방장을 참가시켜 '내 매치'의 진행중/취소 탭도 채운다)
 *
 * 박매니저(MANAGER)는 어떤 경기에도 참가하지 않음 — 시설 관리 역할 전담.
 * 벌크 참가자는 매 경기마다 100명 전체에서 rng로 중복 없이 뽑아 분포를 다양화한다.
 * seed-fixture 단독 실행 시 벌크 유저가 없으면 로그 경고 후 픽스처 유저로 대체.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("seed-fixture")
public class FixtureSeeder implements SeedTask {

    private final PasswordHasher passwordHasher;
    private final SeedProperties seedProperties;
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

        log.info("[FixtureSeeder] 김방장 방장 완료경기 생성 (풋살4/축구3)");
        createHostCompletedAsHost(u, bulk, rng);

        log.info("[FixtureSeeder] 김방장 참가 완료경기 생성 (풋살4/축구4/배드민턴3)");
        createHostCompletedAsParticipant(u, bulk, rng);

        log.info("[FixtureSeeder] /matches 데모용 미래·취소 경기 생성");
        createDemoAndFutureMatches(u, bulk, rng);
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

        // 김방장: 완료 경기가 있는 3종목 모두 자가신고 stat 보유 → 마이페이지 실력 그래프 표시
        addStatIfAbsent(host,     SportType.FUTSAL,      SelfReportedLevel.ADVANCED);
        addStatIfAbsent(host,     SportType.SOCCER,      SelfReportedLevel.INTERMEDIATE);
        addStatIfAbsent(host,     SportType.BADMINTON,   SelfReportedLevel.INTERMEDIATE);

        addStatIfAbsent(manager,  SportType.FUTSAL,      SelfReportedLevel.INTERMEDIATE);
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
                User.local(email, nickname, passwordHasher.hash(seedProperties.fixture().password()), role)
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
        for (int i = 1; i <= 100; i++) {
            userRepository.findByEmail("bulk" + i + SeedConstants.BULK_EMAIL_DOMAIN)
                .ifPresent(result::add);
        }
        return result;
    }

    // ─── 김방장 방장 완료경기: 풋살 4(J4×3+J5×1) + 축구 3(J4×3) ────────
    // 종목별 통계 집계 대상(COMPLETED). 김방장은 host, 참가자는 픽스처 일부 + 벌크.

    private void createHostCompletedAsHost(Fu u, List<User> bulk, Random rng) {
        LocalDate today = SeedConstants.today();

        // 풋살 방장 1: J4 완료+정산 (1주 전) — 정원10, 8명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            prepend(List.of(u.middle, u.beginner, u.rising), pickBulk(bulk, 6, rng, u, u.host, u.middle, u.beginner, u.rising)),
            today.minusDays(7), LocalTime.of(19, 0), SportType.FUTSAL, 10, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "지난 풋살 (1주 전)", "마포 풋살센터", rng));

        // 풋살 방장 2: J4 완료+정산 (3주 전) — 정원8, 6명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            prepend(List.of(u.middle, u.beginner), pickBulk(bulk, 5, rng, u, u.host, u.middle, u.beginner)),
            today.minusDays(20), LocalTime.of(20, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "지난 풋살 (3주 전)", "서초 풋살센터", rng));

        // 풋살 방장 3: J4 완료+정산 (약 6주 전) — 정원6, 5명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            prepend(List.of(u.rising), pickBulk(bulk, 4, rng, u, u.host, u.rising)),
            today.minusDays(40), LocalTime.of(19, 0), SportType.FUTSAL, 6, 12000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "지난 풋살 (6주 전)", "노원 풋살파크", rng));

        // 풋살 방장 4: J5 오늘 정산대기 (COMPLETED, Settlement 없음) — 정원6, 5명
        journeyFactory.createJ5SettlementPending(new JourneyRequest(u.host,
            prepend(List.of(u.middle, u.beginner), pickBulk(bulk, 3, rng, u, u.host, u.middle, u.beginner)),
            today, LocalTime.of(9, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "오늘 풋살 (정산 대기)", "중구 풋살장", rng));

        // 축구 방장 1: J4 완료+정산 (2주 전) — 정원10, 8명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            prepend(List.of(u.declined, u.rising), pickBulk(bulk, 7, rng, u, u.host, u.declined, u.rising)),
            today.minusDays(14), LocalTime.of(10, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "지난 축구 (2주 전)", "용산 축구장", rng));

        // 축구 방장 2: J4 완료+정산 (1달 전) — 정원10, 8명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            prepend(List.of(u.middle, u.declined), pickBulk(bulk, 7, rng, u, u.host, u.middle, u.declined)),
            today.minusDays(30), LocalTime.of(20, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_5, "지난 축구 (1달 전)", "강남 축구장", rng));

        // 축구 방장 3: J4 완료+정산 (약 8주 전) — 정원10, 8명
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(u.host,
            prepend(List.of(u.rising), pickBulk(bulk, 8, rng, u, u.host, u.rising)),
            today.minusDays(55), LocalTime.of(18, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.ANY, "지난 축구 (8주 전)", "송파 축구장", rng));
    }

    // ─── 김방장 참가 완료경기: 풋살 4 + 축구 4 + 배드민턴 3 ─────────────
    // 종목별 통계 집계 대상(COMPLETED). 방장은 벌크 유저, 김방장은 participant.

    private void createHostCompletedAsParticipant(Fu u, List<User> bulk, Random rng) {
        LocalDate today = SeedConstants.today();

        // 풋살 참가 1: J4 (5일 전) 정원8, 6명
        User fh1 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(fh1,
            prepend(List.of(u.host, u.beginner), pickBulk(bulk, 5, rng, u, fh1, u.host, u.beginner)),
            today.minusDays(5), LocalTime.of(19, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 풋살 참가 1", "풋살장 A", rng));

        // 풋살 참가 2: J4 (12일 전) 정원8, 5명
        User fh2 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(fh2,
            prepend(List.of(u.host, u.middle), pickBulk(bulk, 5, rng, u, fh2, u.host, u.middle)),
            today.minusDays(12), LocalTime.of(20, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 풋살 참가 2", "풋살장 B", rng));

        // 풋살 참가 3: J4 (25일 전) 정원10, 8명
        User fh3 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(fh3,
            prepend(List.of(u.host, u.rising, u.declined), pickBulk(bulk, 6, rng, u, fh3, u.host, u.rising, u.declined)),
            today.minusDays(25), LocalTime.of(18, 0), SportType.FUTSAL, 10, 12000,
            SkillLevel.LEVEL_2, SkillLevel.LEVEL_4, "완료 풋살 참가 3", "풋살장 C", rng));

        // 풋살 참가 4: J4 (50일 전) 정원6, 5명
        User fh4 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(fh4,
            prepend(List.of(u.host), pickBulk(bulk, 4, rng, u, fh4, u.host)),
            today.minusDays(50), LocalTime.of(20, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.ANY, "완료 풋살 참가 4", "풋살장 D", rng));

        // 축구 참가 1: J4 (3일 전) 정원10, 8명
        User sh1 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(sh1,
            prepend(List.of(u.host, u.declined), pickBulk(bulk, 7, rng, u, sh1, u.host, u.declined)),
            today.minusDays(3), LocalTime.of(10, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 축구 참가 1", "축구장 A", rng));

        // 축구 참가 2: J4 (18일 전) 정원10, 8명
        User sh2 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(sh2,
            prepend(List.of(u.host, u.rising), pickBulk(bulk, 7, rng, u, sh2, u.host, u.rising)),
            today.minusDays(18), LocalTime.of(14, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "완료 축구 참가 2", "축구장 B", rng));

        // 축구 참가 3: J4 (35일 전) 정원8, 6명
        User sh3 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(sh3,
            prepend(List.of(u.host, u.middle), pickBulk(bulk, 5, rng, u, sh3, u.host, u.middle)),
            today.minusDays(35), LocalTime.of(19, 0), SportType.SOCCER, 8, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 축구 참가 3", "축구장 C", rng));

        // 축구 참가 4: J4 (60일 전) 정원10, 8명
        User sh4 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(sh4,
            prepend(List.of(u.host), pickBulk(bulk, 8, rng, u, sh4, u.host)),
            today.minusDays(60), LocalTime.of(20, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.ANY, "완료 축구 참가 4", "축구장 D", rng));

        // 배드민턴 참가 1: J4 (8일 전) 정원6, 5명
        User bh1 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(bh1,
            prepend(List.of(u.host, u.middle), pickBulk(bulk, 3, rng, u, bh1, u.host, u.middle)),
            today.minusDays(8), LocalTime.of(15, 0), SportType.BADMINTON, 6, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "완료 배드민턴 참가 1", "배드민턴장 A", rng));

        // 배드민턴 참가 2: J4 (22일 전) 정원6, 5명
        User bh2 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(bh2,
            prepend(List.of(u.host), pickBulk(bulk, 4, rng, u, bh2, u.host)),
            today.minusDays(22), LocalTime.of(16, 0), SportType.BADMINTON, 6, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "완료 배드민턴 참가 2", "배드민턴장 B", rng));

        // 배드민턴 참가 3: J4 (45일 전) 정원4(복식), 4명
        User bh3 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ4CompletedSettled(new JourneyRequest(bh3,
            prepend(List.of(u.host), pickBulk(bulk, 2, rng, u, bh3, u.host)),
            today.minusDays(45), LocalTime.of(11, 0), SportType.BADMINTON, 4, 8000,
            SkillLevel.ANY, SkillLevel.ANY, "완료 배드민턴 참가 3", "배드민턴장 C", rng));
    }

    // ─── /matches 데모용 미래·취소 경기 (종목별 통계 미집계) ────────────

    private void createDemoAndFutureMatches(Fu u, List<User> bulk, Random rng) {
        LocalDate today = SeedConstants.today();

        // 강남 퇴근후풋살: 이중수 방장, 정원8, 6명 모집중
        journeyFactory.createJ1Recruiting(new JourneyRequest(u.middle,
            prepend(List.of(u.beginner, u.rising), pickBulk(bulk, 3, rng, u, u.middle, u.beginner, u.rising)),
            today.plusDays(5), LocalTime.of(19, 0), SportType.FUTSAL, 8, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "강남 퇴근후풋살", "강남 퇴근후풋살 센터", rng));

        // 마감임박 인기풋살: 한고수 방장, 정원6, 5명(마감임박)
        journeyFactory.createJ1Recruiting(new JourneyRequest(u.declined,
            prepend(List.of(u.middle, u.beginner, u.rising), pickBulk(bulk, 1, rng, u, u.declined, u.middle, u.beginner, u.rising)),
            today.plusDays(2), LocalTime.of(20, 0), SportType.FUTSAL, 6, 12000,
            SkillLevel.LEVEL_2, SkillLevel.LEVEL_5, "마감임박 인기풋살", "서초 프리미엄 풋살", rng));

        // 모집완료 농구: 정성장 방장, 정원10 가득(방장1+참가9) 확정 — 김방장 참가(진행중 탭)
        journeyFactory.createJ2Confirmed(new JourneyRequest(u.rising,
            prepend(List.of(u.host, u.middle, u.declined), pickBulk(bulk, 6, rng, u, u.rising, u.host, u.middle, u.declined)),
            today.plusDays(4), LocalTime.of(18, 0), SportType.BASKETBALL, 10, 15000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "모집완료 농구 경기", "송파 실내 농구장", rng));

        // 정원가득찬 배드민턴: 이중수 방장, 정원6, 6명 가득 — 김방장 참가
        journeyFactory.createJ3Full(new JourneyRequest(u.middle,
            prepend(List.of(u.host, u.beginner, u.rising, u.declined), pickBulk(bulk, 1, rng, u, u.middle, u.host, u.beginner, u.rising, u.declined)),
            today.plusDays(6), LocalTime.of(14, 0), SportType.BADMINTON, 6, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "정원가득찬 배드민턴", "강남 배드민턴클럽", rng));

        // 다가오는 풋살: 벌크 방장, 김방장 참가(진행중 탭 - 풋살)
        User uh1 = pickOneHost(bulk, rng, u);
        journeyFactory.createJ1Recruiting(new JourneyRequest(uh1,
            prepend(List.of(u.host, u.rising), pickBulk(bulk, 4, rng, u, uh1, u.host, u.rising)),
            today.plusDays(9), LocalTime.of(19, 0), SportType.FUTSAL, 10, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "다가오는 주말 풋살", "풋살장 E", rng));

        // 취소된 풋살: 김방장 방장 → 김방장 '내 매치' 취소 탭(방장)
        journeyFactory.createJCancelled(new JourneyRequest(u.host,
            List.of(u.rising, u.declined),
            today.minusDays(21), LocalTime.of(19, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "취소된 풋살 (방장)", "취소 풋살장", rng));

        // 취소된 축구: 벌크 방장, 김방장 참가 → 김방장 취소 탭(참가)
        User ch1 = pickOneHost(bulk, rng, u);
        journeyFactory.createJCancelled(new JourneyRequest(ch1,
            prepend(List.of(u.host), pickBulk(bulk, 2, rng, u, ch1, u.host)),
            today.minusDays(15), LocalTime.of(18, 0), SportType.SOCCER, 10, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "취소된 축구 (참가)", "취소 축구장", rng));

        // 조기취소 농구: 정성장 방장, 저조한 참가로 취소
        journeyFactory.createJCancelled(new JourneyRequest(u.rising,
            List.of(u.beginner),
            today.minusDays(28), LocalTime.of(14, 0), SportType.BASKETBALL, 10, 15000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "조기취소 농구", "영등포 체육관", rng));
    }

    // ─── Utilities ───────────────────────────────────────────────────

    /**
     * 벌크 유저 100명 전체에서 rng로 셔플 후 count명을 중복 없이 뽑는다.
     * excludes에 포함된 유저(벌크 방장 등)는 후보에서 제외.
     * 벌크 유저가 없으면 픽스처 유저 일부로 fallback.
     */
    private List<User> pickBulk(List<User> bulk, int count, Random rng, Fu u, User... excludes) {
        if (bulk.isEmpty()) {
            return fallbackParticipants(u, count, excludes);
        }
        List<User> pool = new ArrayList<>(bulk);
        for (User ex : excludes) {
            pool.remove(ex);
        }
        Collections.shuffle(pool, rng);
        return new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
    }

    private List<User> fallbackParticipants(Fu u, int count, User... excludes) {
        List<User> candidates = new ArrayList<>(List.of(u.middle, u.beginner, u.rising, u.declined));
        for (User ex : excludes) {
            candidates.remove(ex);
        }
        return new ArrayList<>(candidates.subList(0, Math.min(count, candidates.size())));
    }

    /** 벌크 유저 중 랜덤으로 방장 1명 선택. 없으면 픽스처 유저 fallback. */
    private User pickOneHost(List<User> bulk, Random rng, Fu u) {
        if (bulk.isEmpty()) return u.middle;
        return bulk.get(rng.nextInt(bulk.size()));
    }

    /** fixed 리스트 앞에 두고 bulk 리스트를 뒤에 합친 새 리스트 반환. */
    private List<User> prepend(List<User> fixed, List<User> extra) {
        List<User> result = new ArrayList<>(fixed);
        result.addAll(extra);
        return result;
    }

    /** 픽스처 유저 6명 묶음 레코드. */
    private record Fu(User manager, User host, User middle, User beginner, User rising, User declined) {}
}
