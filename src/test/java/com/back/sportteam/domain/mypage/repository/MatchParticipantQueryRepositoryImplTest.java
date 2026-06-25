package com.back.sportteam.domain.mypage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchCreateCommand;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.RequiredGender;
import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.MyMatchStatus;
import com.back.sportteam.domain.mypage.dto.request.MyMatchCondition;
import com.back.sportteam.domain.mypage.dto.response.MyMatchResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MatchParticipantQueryRepositoryImplTest {

    private static final String USER_A = UUID.randomUUID().toString();
    private static final String USER_B = UUID.randomUUID().toString();

    private static final LocalDate DATE_EARLY = LocalDate.of(2099, Month.JUNE, 9);
    private static final LocalDate DATE_MID = LocalDate.of(2099, Month.JUNE, 10);
    private static final LocalDate DATE_LATE = LocalDate.of(2099, Month.JUNE, 11);
    private static final LocalTime START_TIME = LocalTime.of(10, 0);
    private static final LocalTime END_TIME = LocalTime.of(12, 0);

    @Autowired
    private MatchParticipantQueryRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    void 필터_없이_조회하면_해당_사용자의_전체_참여_이력을_반환한다() {
        Match futsal = persistMatch(SportType.FUTSAL, DATE_MID);
        Match basketball = persistMatch(SportType.BASKETBALL, DATE_LATE);
        persistActiveHost(futsal, USER_A);
        persistActiveParticipant(basketball, USER_A);
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_B);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, null, null, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(r -> r.matchId() != null);
    }

    @Test
    void 필터_없이_조회하면_CANCELLED_참여이력도_포함된다() {
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);
        persistCancelledParticipant(persistMatch(SportType.FUTSAL, DATE_EARLY), USER_A);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, null, null, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).anyMatch(r -> r.myMatchStatus() == MyMatchStatus.CANCELLED);
        assertThat(result.getContent()).anyMatch(r -> r.myMatchStatus() == MyMatchStatus.PARTICIPATING);
    }

    @Test
    void 다른_사용자의_참여_이력은_조회되지_않는다() {
        Match match = persistMatch(SportType.FUTSAL, DATE_MID);
        persistActiveHost(match, USER_B);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, null, null, 0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void sportType_필터로_해당_종목_경기만_조회된다() {
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);
        persistActiveHost(persistMatch(SportType.BASKETBALL, DATE_MID), USER_A);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(SportType.FUTSAL, null, null, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).sportType()).isEqualTo(SportType.FUTSAL);
    }

    @Test
    void role_HOST_필터로_방장으로_참여한_경기만_조회된다() {
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);
        persistActiveParticipant(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, null, MatchParticipantRole.HOST, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).role()).isEqualTo(MatchParticipantRole.HOST);
    }

    @Test
    void role_PARTICIPANT_필터로_참가자로_참여한_경기만_조회된다() {
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);
        persistActiveParticipant(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, null, MatchParticipantRole.PARTICIPANT, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).role()).isEqualTo(MatchParticipantRole.PARTICIPANT);
    }

    @Test
    void PARTICIPATING_필터로_참여예정_경기만_조회된다() {
        Match recruiting = persistMatch(SportType.FUTSAL, DATE_MID);
        Match confirmed = persistMatch(SportType.FUTSAL, DATE_MID);
        confirmed.confirm(LocalDateTime.of(2099, Month.JUNE, 1, 10, 0));
        Match completed = persistMatch(SportType.FUTSAL, DATE_MID);
        ReflectionTestUtils.setField(completed, "status", MatchStatus.COMPLETED);

        persistActiveParticipant(recruiting, USER_A);
        persistActiveParticipant(confirmed, USER_A);
        persistActiveParticipant(completed, USER_A);
        em.flush();

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, MyMatchStatus.PARTICIPATING, null, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(r -> r.myMatchStatus() == MyMatchStatus.PARTICIPATING);
    }

    @Test
    void COMPLETED_필터로_참여완료_경기만_조회된다() {
        Match completed = persistMatch(SportType.FUTSAL, DATE_MID);
        ReflectionTestUtils.setField(completed, "status", MatchStatus.COMPLETED);
        Match recruiting = persistMatch(SportType.FUTSAL, DATE_MID);

        persistActiveParticipant(completed, USER_A);
        persistActiveParticipant(recruiting, USER_A);
        em.flush();

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, MyMatchStatus.COMPLETED, null, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).myMatchStatus()).isEqualTo(MyMatchStatus.COMPLETED);
    }

    @Test
    void CANCELLED_필터로_취소된_경기만_조회된다() {
        Match recruiting = persistMatch(SportType.FUTSAL, DATE_MID);
        persistCancelledParticipant(recruiting, USER_A);
        persistActiveParticipant(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, MyMatchStatus.CANCELLED, null, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).myMatchStatus()).isEqualTo(MyMatchStatus.CANCELLED);
    }

    @Test
    void matchDate_기준_내림차순으로_정렬된다() {
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_EARLY), USER_A);
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_LATE), USER_A);
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, null, null, 0, 10));

        assertThat(result.getContent().get(0).matchDate()).isEqualTo(DATE_LATE);
        assertThat(result.getContent().get(1).matchDate()).isEqualTo(DATE_MID);
        assertThat(result.getContent().get(2).matchDate()).isEqualTo(DATE_EARLY);
    }

    @Test
    void 페이지네이션이_올바르게_동작한다() {
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_EARLY), USER_A);
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_LATE), USER_A);

        Page<MyMatchResponse> firstPage = repository.findMyMatches(USER_A, condition(null, null, null, 0, 2));
        Page<MyMatchResponse> secondPage = repository.findMyMatches(USER_A, condition(null, null, null, 1, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(1);
    }

    @Test
    void 참여_이력이_없으면_빈_Page를_반환한다() {
        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, null, null, 0, 10));

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void 응답_필드가_올바르게_매핑된다() {
        Match match = persistMatch(SportType.FUTSAL, DATE_MID);
        persistActiveHost(match, USER_A);

        MyMatchResponse r = repository.findMyMatches(USER_A, condition(null, null, null, 0, 10))
                .getContent().get(0);

        assertThat(r.matchId()).isEqualTo(match.getId());
        assertThat(r.title()).isEqualTo("테스트 경기");
        assertThat(r.sportType()).isEqualTo(SportType.FUTSAL);
        assertThat(r.myMatchStatus()).isEqualTo(MyMatchStatus.PARTICIPATING);
        assertThat(r.role()).isEqualTo(MatchParticipantRole.HOST);
        assertThat(r.matchDate()).isEqualTo(DATE_MID);
        assertThat(r.startTime()).isEqualTo(START_TIME);
        assertThat(r.endTime()).isEqualTo(END_TIME);
    }

    @Test
    void 경기_자체가_취소되면_CANCELLED로_조회된다() {
        Match cancelledMatch = persistMatch(SportType.FUTSAL, DATE_MID);
        ReflectionTestUtils.setField(cancelledMatch, "status", MatchStatus.CANCELLED);
        persistActiveParticipant(cancelledMatch, USER_A);
        em.flush();

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A, condition(null, MyMatchStatus.CANCELLED, null, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).myMatchStatus()).isEqualTo(MyMatchStatus.CANCELLED);
    }

    @Test
    void 복합_필터가_동시에_적용된다() {
        persistActiveHost(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);
        persistActiveParticipant(persistMatch(SportType.FUTSAL, DATE_MID), USER_A);
        persistActiveHost(persistMatch(SportType.BASKETBALL, DATE_MID), USER_A);

        Page<MyMatchResponse> result = repository.findMyMatches(USER_A,
                condition(SportType.FUTSAL, null, MatchParticipantRole.HOST, 0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).sportType()).isEqualTo(SportType.FUTSAL);
        assertThat(result.getContent().get(0).role()).isEqualTo(MatchParticipantRole.HOST);
    }

    private Match persistMatch(SportType sportType, LocalDate matchDate) {
        Match match = Match.create(MatchCreateCommand.builder()
                .reservationId(UUID.randomUUID().toString())
                .hostId(UUID.randomUUID().toString())
                .title("테스트 경기")
                .sportType(sportType)
                .capacity(10)
                .feePerPerson(10_000)
                .minSkillLevel(SkillLevel.ANY)
                .maxSkillLevel(SkillLevel.ANY)
                .requiredGender(RequiredGender.ANY)
                .matchDate(matchDate)
                .startTime(START_TIME)
                .endTime(END_TIME)
                .recruitDeadline(LocalDateTime.of(2099, Month.JUNE, 8, 10, 0))
                .participantCancelDeadline(LocalDateTime.of(2099, Month.JUNE, 9, 10, 0))

                .hostCancelDeadline(LocalDateTime.of(2099, Month.JUNE, 9, 10, 0))
                .build());
        em.persist(match);
        return match;
    }

    private void persistActiveHost(Match match, String userId) {
        MatchParticipant p = MatchParticipant.host(match, userId);
        p.activate();
        em.persist(p);
    }

    private void persistActiveParticipant(Match match, String userId) {
        MatchParticipant p = MatchParticipant.participant(match, userId);
        p.activate();
        em.persist(p);
    }

    private void persistCancelledParticipant(Match match, String userId) {
        MatchParticipant p = MatchParticipant.participant(match, userId);
        p.cancel();
        em.persist(p);
    }

    private MyMatchCondition condition(SportType sportType, MyMatchStatus myMatchStatus,
                                       MatchParticipantRole role, int page, int size) {
        return new MyMatchCondition(sportType, myMatchStatus, role, page, size);
    }
}
