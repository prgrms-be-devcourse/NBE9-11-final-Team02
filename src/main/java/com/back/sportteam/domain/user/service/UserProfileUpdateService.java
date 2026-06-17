package com.back.sportteam.domain.user.service;

import com.back.sportteam.domain.user.dto.request.UserProfileUpdateRequest;
import com.back.sportteam.domain.user.dto.response.UserProfileUpdateResponse;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileUpdateService {

    private final UserRepository userRepository;

    @Transactional
    public UserProfileUpdateResponse updateMyProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(
                request.nickname(),
                request.position(),
                request.activeRegion(),
                request.preferredSport(),
                request.profileImg()
        );

        return UserProfileUpdateResponse.from(user);
    }
}