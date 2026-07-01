package com.back.sportteam.seed.journey;

import com.back.sportteam.domain.match.entity.SkillLevel;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

/**
 * JourneyFactory 에 전달되는 매칭 생성 파라미터.
 * BulkSeeder / FixtureSeeder 양쪽에서 빌드하여 사용한다.
 */
public record JourneyRequest(
        User host,
        List<User> participants,  // 방장 제외 참가자 목록
        LocalDate matchDate,
        LocalTime startTime,
        SportType sportType,
        int capacity,             // 방장 포함 전체 정원
        int feePerPerson,
        SkillLevel minLevel,
        SkillLevel maxLevel,
        String title,
        String facilityName,
        Random rng
) {}
