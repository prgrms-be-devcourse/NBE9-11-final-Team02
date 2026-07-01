package com.back.sportteam.seed.fixture;

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
import java.util.List;
import java.util.Random;

/**
 * 픽스처(시나리오) 더미 데이터 생성. seed-fixture 프로파일에서만 활성화.
 *
 * 생성 내용
 *   · 픽스처 유저 6명 (박매니저/김방장/이중수/최초보/정성장/한고수)
 *   · 방장 전용 Journey 10개 (J1×2, J2×1, J3×1, J4×3, J5×1, J6×1, J7×1)
 *   · 데모 매칭 4개 (나머지 유저들이 방장 역할)
 *
 * 멱등성: 이메일 중복 체크(유저), SeedRun 마커(전체)
 * 비밀번호: DUMMY_PW_HASH 는 실제 인증 불가 — 시연용 API 테스트 전용
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("seed-fixture")
public class FixtureSeeder implements SeedTask {

    // BCrypt 포맷이지만 복호화 불가 — 로그인 목적 아님
    private static final String DUMMY_PW_HASH =
        "$2a$10$seedFixturePwHashXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.";

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
        Fu users = createFixtureUsers();

        log.info("[FixtureSeeder] 방장 전용 Journey 10개 생성");
        createHostJourneys(users, rng);

        log.info("[FixtureSeeder] 데모 매칭 4개 생성");
        createDemoMatches(users, rng);
    }

    // ─── 부분 실패 가드 ──────────────────────────────────────────────

    private void guardAgainstPartialRun() {
        // 마커(seed_run FIXTURE)가 없는데 픽스처 유저가 있다 = 이전 실행이 journey 도중 실패한 흔적.
        // 재실행 시 journey가 중복 생성되므로 진행을 막는다.
        // 복구: fixture 데이터(@seed.sportteam.local 유저 및 그 매칭)를 정리한 뒤 재실행할 것.
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
                User.local(email, nickname, DUMMY_PW_HASH, role)
            ));
    }

    private void addStatIfAbsent(User user, SportType sport, SelfReportedLevel level) {
        userSportStatRepository.findByUser_IdAndSportType(user.getId(), sport)
            .orElseGet(() -> userSportStatRepository.save(
                UserSportStat.create(user, sport, level)
            ));
    }

    // ─── 방장 전용 Journey 10개 ───────────────────────────────────────

    private void createHostJourneys(Fu u, Random rng) {
        LocalDate today = SeedConstants.today();

        // J1 × 2: 모집중 (미래)
        journeyFactory.createJ1Recruiting(req(u.host, List.of(u.middle, u.beginner),
            today.plusDays(7), LocalTime.of(19, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "방장의 퇴근후 풋살", "풋살파크 강남점", rng));

        journeyFactory.createJ1Recruiting(req(u.host, List.of(u.rising),
            today.plusDays(14), LocalTime.of(10, 0), SportType.SOCCER, 8, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "주말 오전 축구", "강남 축구장", rng));

        // J2 × 1: 확정 (미래)
        journeyFactory.createJ2Confirmed(req(u.host, List.of(u.middle, u.beginner, u.rising),
            today.plusDays(3), LocalTime.of(20, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "방장 확정 풋살", "서초 풋살센터", rng));

        // J3 × 1: 정원가득 (미래)
        journeyFactory.createJ3Full(req(u.host,
            List.of(u.manager, u.middle, u.beginner, u.rising, u.declined),
            today.plusDays(10), LocalTime.of(18, 0), SportType.FUTSAL, 6, 12000,
            SkillLevel.LEVEL_2, SkillLevel.LEVEL_4, "정원 가득찬 풋살", "송파 풋살장", rng));

        // J4 × 3: 완료+정산 (과거)
        journeyFactory.createJ4CompletedSettled(req(u.host, List.of(u.middle, u.beginner, u.rising),
            today.minusDays(14), LocalTime.of(19, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "지난 풋살 (2주 전)", "마포 풋살센터", rng));

        journeyFactory.createJ4CompletedSettled(req(u.host, List.of(u.declined, u.rising, u.middle),
            today.minusDays(30), LocalTime.of(20, 0), SportType.SOCCER, 8, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_5, "지난 축구 (1달 전)", "용산 축구장", rng));

        journeyFactory.createJ4CompletedSettled(req(u.host, List.of(u.manager, u.beginner),
            today.minusDays(45), LocalTime.of(10, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.ANY, "지난 풋살 (45일 전)", "강서 풋살장", rng));

        // J5 × 1: 오늘, 정산 대기
        journeyFactory.createJ5SettlementPending(req(u.host, List.of(u.middle, u.beginner),
            today, LocalTime.of(9, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "오늘 풋살 (정산 대기)", "노원 풋살파크", rng));

        // J6 × 1: 취소 + 환불 (과거)
        journeyFactory.createJCancelled(req(u.host, List.of(u.rising, u.declined),
            today.minusDays(21), LocalTime.of(19, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "취소된 풋살", "중구 풋살장", rng));

        // J7 × 1: 조기취소 (과거, 참가자 적음)
        journeyFactory.createJCancelled(req(u.host, List.of(u.beginner),
            today.minusDays(28), LocalTime.of(14, 0), SportType.BASKETBALL, 8, 15000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "조기취소 농구", "영등포 체육관", rng));
    }

    // ─── 데모 매칭 4개 ───────────────────────────────────────────────

    private void createDemoMatches(Fu u, Random rng) {
        LocalDate today = SeedConstants.today();

        // 강남 퇴근후풋살: 이중수 방장, 모집중
        journeyFactory.createJ1Recruiting(req(u.middle, List.of(u.beginner, u.rising),
            today.plusDays(5), LocalTime.of(19, 0), SportType.FUTSAL, 6, 10000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "강남 퇴근후풋살", "강남 퇴근후풋살 센터", rng));

        // 마감임박 인기매칭: 한고수 방장, 거의 가득
        journeyFactory.createJ1Recruiting(req(u.declined,
            List.of(u.manager, u.middle, u.beginner, u.rising),
            today.plusDays(2), LocalTime.of(20, 0), SportType.FUTSAL, 6, 12000,
            SkillLevel.LEVEL_2, SkillLevel.LEVEL_5, "마감임박 인기풋살", "서초 프리미엄 풋살", rng));

        // 모집완료 농구: 정성장 방장, 확정
        journeyFactory.createJ2Confirmed(req(u.rising, List.of(u.host, u.middle, u.declined),
            today.plusDays(4), LocalTime.of(18, 0), SportType.BASKETBALL, 8, 15000,
            SkillLevel.ANY, SkillLevel.LEVEL_3, "모집완료 농구 경기", "송파 실내 농구장", rng));

        // 정원가득찬 배드민턴: 박매니저 방장, 정원 가득
        journeyFactory.createJ3Full(req(u.manager,
            List.of(u.host, u.middle, u.beginner, u.rising, u.declined),
            today.plusDays(6), LocalTime.of(14, 0), SportType.BADMINTON, 6, 8000,
            SkillLevel.ANY, SkillLevel.LEVEL_4, "정원가득찬 배드민턴", "강남 배드민턴클럽", rng));
    }

    // ─── Helper ──────────────────────────────────────────────────────

    private JourneyRequest req(
        User host, List<User> participants,
        LocalDate date, LocalTime start,
        SportType sport, int capacity, int fee,
        SkillLevel min, SkillLevel max,
        String title, String facilityName,
        Random rng
    ) {
        return new JourneyRequest(host, participants, date, start, sport, capacity, fee,
            min, max, title, facilityName, rng);
    }

    /** 픽스처 유저 6명 묶음 레코드. */
    private record Fu(User manager, User host, User middle, User beginner, User rising, User declined) {}
}
