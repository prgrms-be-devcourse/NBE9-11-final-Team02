package com.back.sportteam.domain.mypage.service;

import com.back.sportteam.domain.match.entity.MatchParticipantRole;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.entity.MatchStatus;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.mypage.dto.response.MyRecordResponse;
import com.back.sportteam.domain.mypage.repository.MyPageRecordRepository;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageRecordService {

    private final MyPageRecordRepository myPageRecordRepository;
    private final UserRepository userRepository;
    private final UserSportStatRepository userSportStatRepository;

    @Transactional(readOnly = true)
    public MyRecordResponse getMyRecord(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        int hostedMatchCount = myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(
                userId, MatchParticipantStatus.ACTIVE, MatchStatus.COMPLETED, MatchParticipantRole.HOST);
        int participatedMatchCount = myPageRecordRepository.countByUserIdAndStatusAndMatch_StatusAndRole(
                userId, MatchParticipantStatus.ACTIVE, MatchStatus.COMPLETED, MatchParticipantRole.PARTICIPANT);
        int totalMatchCount = hostedMatchCount + participatedMatchCount;

        List<MyRecordResponse.SportStat> sportStats = myPageRecordRepository
                .findSportStats(userId, MatchParticipantStatus.ACTIVE, MatchStatus.COMPLETED)
                .stream()
                .map(row -> new MyRecordResponse.SportStat((SportType) row[0], (Long) row[1]))
                .toList();

        LocalDate fromDate = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        List<MyRecordResponse.MonthlyStat> monthlyStats = myPageRecordRepository
                .findMonthlyStats(userId, MatchParticipantStatus.ACTIVE, MatchStatus.COMPLETED, fromDate)
                .stream()
                .map(row -> new MyRecordResponse.MonthlyStat((Integer) row[0], (Integer) row[1], (Long) row[2]))
                .toList();

        Set<String> participatedSportTypeNames = sportStats.stream()
                .map(s -> s.sportType().name())
                .collect(Collectors.toSet());

        List<MyRecordResponse.SkillStat> skillStats = userSportStatRepository
                .findByUser_IdAndReviewCountGreaterThan(userId, 0)
                .stream()
                .filter(stat -> participatedSportTypeNames.contains(stat.getSportType().name()))
                .map(MyRecordResponse.SkillStat::from)
                .toList();

        return MyRecordResponse.of(
                totalMatchCount,
                hostedMatchCount,
                participatedMatchCount,
                sportStats,
                monthlyStats,
                user,
                skillStats
        );
    }
}
