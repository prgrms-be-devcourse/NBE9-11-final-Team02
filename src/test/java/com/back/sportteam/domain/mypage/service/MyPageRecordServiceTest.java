package com.back.sportteam.domain.mypage.service;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.response.MyRecordResponse;
import com.back.sportteam.domain.mypage.repository.MyPageRecordRepository;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.user.entity.SelfReportedLevel;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyPageRecordServiceTest {

    private static final String USER_ID = "user-001";

    @Mock private MyPageRecordRepository myPageRecordRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserSportStatRepository userSportStatRepository;

    @InjectMocks
    private MyPageRecordService myPageRecordService;

    @Test
    void 운동기록_조회시_존재하지_않는_유저면_예외가_발생한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPageRecordService.getMyRecord(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 전체_경기수는_개최한_경기수와_참가한_경기수의_합이다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(
                USER_ID, MatchParticipantStatus.ACTIVE, MatchStatus.COMPLETED, MatchParticipantRole.HOST))
                .thenReturn(3);
        when(myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(
                USER_ID, MatchParticipantStatus.ACTIVE, MatchStatus.COMPLETED, MatchParticipantRole.PARTICIPANT))
                .thenReturn(7);
        when(myPageRecordRepository.findSportStats(any(), any(), any())).thenReturn(List.of());
        when(myPageRecordRepository.findMonthlyStats(any(), any(), any(), any())).thenReturn(List.of());
        when(userSportStatRepository.findByUser_IdAndReviewCountGreaterThan(any(), anyInt())).thenReturn(List.of());

        MyRecordResponse response = myPageRecordService.getMyRecord(USER_ID);

        assertThat(response.hostedMatchCount()).isEqualTo(3);
        assertThat(response.participatedMatchCount()).isEqualTo(7);
        assertThat(response.totalMatchCount()).isEqualTo(10);
    }

    // TODO : 참가이력만 있어도 skillStats에 포함되도록 개선
    @Test
    void 참가이력과_리뷰가_모두_있는_종목만_skillStats에_포함된다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(any(), any(), any(), any())).thenReturn(0);
        List<Object[]> sportStatRows = new ArrayList<>();
        sportStatRows.add(new Object[]{SportType.FUTSAL, 5L});
        sportStatRows.add(new Object[]{SportType.BASKETBALL, 2L});
        when(myPageRecordRepository.findSportStats(any(), any(), any())).thenReturn(sportStatRows);
        when(myPageRecordRepository.findMonthlyStats(any(), any(), any(), any())).thenReturn(List.of());
        UserSportStat futsalStat = sportStat(SportType.FUTSAL);
        UserSportStat tennisStat = sportStat(SportType.TENNIS);
        when(userSportStatRepository.findByUser_IdAndReviewCountGreaterThan(any(), anyInt()))
                .thenReturn(List.of(futsalStat, tennisStat));

        MyRecordResponse response = myPageRecordService.getMyRecord(USER_ID);

        assertThat(response.skillStats()).hasSize(1);
        assertThat(response.skillStats().get(0).sportType()).isEqualTo(SportType.FUTSAL);
    }

    @Test
    void 참가이력이_있어도_리뷰가_없으면_skillStats에서_제외된다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(any(), any(), any(), any())).thenReturn(0);
        List<Object[]> sportStatRows = new ArrayList<>();
        sportStatRows.add(new Object[]{SportType.FUTSAL, 3L});
        when(myPageRecordRepository.findSportStats(any(), any(), any())).thenReturn(sportStatRows);
        when(myPageRecordRepository.findMonthlyStats(any(), any(), any(), any())).thenReturn(List.of());
        when(userSportStatRepository.findByUser_IdAndReviewCountGreaterThan(USER_ID, 0))
                .thenReturn(List.of());

        MyRecordResponse response = myPageRecordService.getMyRecord(USER_ID);

        assertThat(response.skillStats()).isEmpty();
    }

    @Test
    void mannerStat은_유저의_매너점수와_매너리뷰수를_반환한다() {
        User user = user();
        user.addMannerRating(new BigDecimal("4.5"));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(any(), any(), any(), any())).thenReturn(0);
        when(myPageRecordRepository.findSportStats(any(), any(), any())).thenReturn(List.of());
        when(myPageRecordRepository.findMonthlyStats(any(), any(), any(), any())).thenReturn(List.of());
        when(userSportStatRepository.findByUser_IdAndReviewCountGreaterThan(any(), anyInt())).thenReturn(List.of());

        MyRecordResponse response = myPageRecordService.getMyRecord(USER_ID);

        assertThat(response.mannerStat().mannerScore()).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(response.mannerStat().mannerReviewCount()).isEqualTo(1);
    }

    @Test
    void 리뷰는_있지만_참가이력이_없는_종목은_skillStats에서_제외된다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(any(), any(), any(), any())).thenReturn(0);
        when(myPageRecordRepository.findSportStats(any(), any(), any())).thenReturn(List.of());
        when(myPageRecordRepository.findMonthlyStats(any(), any(), any(), any())).thenReturn(List.of());
        UserSportStat tennisStat = sportStat(SportType.TENNIS);
        when(userSportStatRepository.findByUser_IdAndReviewCountGreaterThan(any(), anyInt()))
                .thenReturn(List.of(tennisStat));

        MyRecordResponse response = myPageRecordService.getMyRecord(USER_ID);

        assertThat(response.skillStats()).isEmpty();
    }

    @Test
    void 완료된_경기가_없으면_totalMatchCount는_0이다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(any(), any(), any(), any())).thenReturn(0);
        when(myPageRecordRepository.findSportStats(any(), any(), any())).thenReturn(List.of());
        when(myPageRecordRepository.findMonthlyStats(any(), any(), any(), any())).thenReturn(List.of());
        when(userSportStatRepository.findByUser_IdAndReviewCountGreaterThan(any(), anyInt())).thenReturn(List.of());

        MyRecordResponse response = myPageRecordService.getMyRecord(USER_ID);

        assertThat(response.totalMatchCount()).isZero();
        assertThat(response.sportStats()).isEmpty();
        assertThat(response.monthlyStats()).isEmpty();
        assertThat(response.skillStats()).isEmpty();
    }

    private User user() {
        return User.local("test@test.com", "테스터", "password", UserRole.USER);
    }

    private UserSportStat sportStat(SportType sportType) {
        UserSportStat stat = org.mockito.Mockito.mock(UserSportStat.class);
        when(stat.getSportType()).thenReturn(sportType);
        when(stat.getPosition()).thenReturn("FW");
        when(stat.getSkillRating()).thenReturn(new BigDecimal("3.50"));
        when(stat.getReviewCount()).thenReturn(3);
        return stat;
    }
}
