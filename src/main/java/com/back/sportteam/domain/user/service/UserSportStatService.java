package com.back.sportteam.domain.user.service;

import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.dto.request.SportStatRegisterRequest;
import com.back.sportteam.domain.user.dto.response.SportStatRegisterResponse;
import com.back.sportteam.domain.user.dto.response.SportStatResponse;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserSportStat;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.domain.user.repository.UserSportStatRepository;
import com.back.sportteam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSportStatService {

    private final UserRepository userRepository;
    private final UserSportStatRepository userSportStatRepository;

    @Transactional
    public SportStatRegisterResponse registerSportStats(String userId, SportStatRegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        List<SportType> requestedTypes = request.stats().stream()
                .map(SportStatRegisterRequest.SportStatItem::sportType)
                .toList();
        if (requestedTypes.size() != new HashSet<>(requestedTypes).size()) {
            throw new BusinessException(UserErrorCode.SPORT_STAT_ALREADY_EXISTS);
        }

        List<UserSportStat> saved = new ArrayList<>();
        for (SportStatRegisterRequest.SportStatItem item : request.stats()) {
            if (userSportStatRepository.findByUser_IdAndSportType(userId, item.sportType()).isPresent()) {
                throw new BusinessException(UserErrorCode.SPORT_STAT_ALREADY_EXISTS);
            }
            UserSportStat stat = UserSportStat.create(user, item.sportType(), item.selfReportedLevel());
            saved.add(userSportStatRepository.save(stat));
        }

        return SportStatRegisterResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SportStatResponse> getSportStats(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        Map<SportType, UserSportStat> registeredMap = userSportStatRepository.findByUser_Id(userId)
                .stream()
                .collect(Collectors.toMap(UserSportStat::getSportType, stat -> stat));

        return List.of(SportType.values()).stream()
                .map(sportType -> registeredMap.containsKey(sportType)
                        ? SportStatResponse.registered(registeredMap.get(sportType))
                        : SportStatResponse.unregistered(sportType))
                .toList();
    }
}
