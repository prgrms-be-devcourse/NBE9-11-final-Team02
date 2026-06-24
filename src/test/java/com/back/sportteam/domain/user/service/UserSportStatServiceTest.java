package com.back.sportteam.domain.user.service;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.dto.request.SportStatRegisterRequest;
import com.back.sportteam.domain.user.dto.response.SportStatRegisterResponse;
import com.back.sportteam.domain.user.dto.response.SportStatResponse;
import com.back.sportteam.domain.user.entity.SelfReportedLevel;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSportStatServiceTest {

    private static final String USER_ID = "user-001";

    @Mock private UserRepository userRepository;
    @Mock private UserSportStatRepository userSportStatRepository;

    @InjectMocks
    private UserSportStatService userSportStatService;

    @Test
    void 존재하지_않는_유저가_종목_실력을_등록하면_예외가_발생한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        SportStatRegisterRequest request = new SportStatRegisterRequest(
                List.of(new SportStatRegisterRequest.SportStatItem(SportType.FUTSAL, SelfReportedLevel.INTERMEDIATE))
        );

        assertThatThrownBy(() -> userSportStatService.registerSportStats(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 종목_실력을_정상_등록하면_등록된_종목과_초기점수를_반환한다() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSportStatRepository.findByUser_IdAndSportType(eq(USER_ID), eq(SportType.FUTSAL)))
                .thenReturn(Optional.empty());
        when(userSportStatRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SportStatRegisterRequest request = new SportStatRegisterRequest(
                List.of(new SportStatRegisterRequest.SportStatItem(SportType.FUTSAL, SelfReportedLevel.INTERMEDIATE))
        );

        SportStatRegisterResponse response = userSportStatService.registerSportStats(USER_ID, request);

        assertThat(response.stats()).hasSize(1);
        assertThat(response.stats().get(0).sportType()).isEqualTo(SportType.FUTSAL);
    }

    @Test
    void 여러_종목을_한번에_등록할_수_있다() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSportStatRepository.findByUser_IdAndSportType(any(), any())).thenReturn(Optional.empty());
        when(userSportStatRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SportStatRegisterRequest request = new SportStatRegisterRequest(List.of(
                new SportStatRegisterRequest.SportStatItem(SportType.FUTSAL, SelfReportedLevel.BEGINNER),
                new SportStatRegisterRequest.SportStatItem(SportType.BASKETBALL, SelfReportedLevel.ADVANCED)
        ));

        SportStatRegisterResponse response = userSportStatService.registerSportStats(USER_ID, request);

        assertThat(response.stats()).hasSize(2);
    }

    @Test
    void 이미_등록된_종목을_다시_등록하면_예외가_발생한다() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        UserSportStat existing = UserSportStat.create(user, SportType.FUTSAL, SelfReportedLevel.INTERMEDIATE);
        when(userSportStatRepository.findByUser_IdAndSportType(USER_ID, SportType.FUTSAL))
                .thenReturn(Optional.of(existing));

        SportStatRegisterRequest request = new SportStatRegisterRequest(
                List.of(new SportStatRegisterRequest.SportStatItem(SportType.FUTSAL, SelfReportedLevel.BEGINNER))
        );

        assertThatThrownBy(() -> userSportStatService.registerSportStats(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.SPORT_STAT_ALREADY_EXISTS));
    }

    @Test
    void 요청_리스트에_같은_종목이_중복으로_들어오면_예외가_발생한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

        SportStatRegisterRequest request = new SportStatRegisterRequest(List.of(
                new SportStatRegisterRequest.SportStatItem(SportType.FUTSAL, SelfReportedLevel.BEGINNER),
                new SportStatRegisterRequest.SportStatItem(SportType.FUTSAL, SelfReportedLevel.INTERMEDIATE)
        ));

        assertThatThrownBy(() -> userSportStatService.registerSportStats(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.SPORT_STAT_ALREADY_EXISTS));
    }

    @Test
    void 종목_실력_조회시_전체_종목과_등록_여부를_반환한다() {
        User user = user();
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        UserSportStat futsalStat = UserSportStat.create(user, SportType.FUTSAL, SelfReportedLevel.INTERMEDIATE);
        when(userSportStatRepository.findByUser_Id(USER_ID)).thenReturn(List.of(futsalStat));

        List<SportStatResponse> response = userSportStatService.getSportStats(USER_ID);

        assertThat(response).hasSize(SportType.values().length);

        SportStatResponse futsal = response.stream()
                .filter(r -> r.sportType() == SportType.FUTSAL)
                .findFirst().orElseThrow();
        assertThat(futsal.registered()).isTrue();
        assertThat(futsal.selfReportedLevel()).isEqualTo(SelfReportedLevel.INTERMEDIATE);

        SportStatResponse basketball = response.stream()
                .filter(r -> r.sportType() == SportType.BASKETBALL)
                .findFirst().orElseThrow();
        assertThat(basketball.registered()).isFalse();
        assertThat(basketball.selfReportedLevel()).isNull();
    }

    @Test
    void 종목_실력_조회시_존재하지_않는_유저면_예외가_발생한다() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userSportStatService.getSportStats(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 등록된_종목이_없으면_전체_종목이_미등록으로_반환된다() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userSportStatRepository.findByUser_Id(USER_ID)).thenReturn(List.of());

        List<SportStatResponse> response = userSportStatService.getSportStats(USER_ID);

        assertThat(response).hasSize(SportType.values().length);
        assertThat(response).allMatch(r -> !r.registered());
    }

    private User user() {
        return User.local("test@test.com", "테스터", "password", UserRole.USER);
    }
}
