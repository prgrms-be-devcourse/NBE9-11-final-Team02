package com.back.sportteam.domain.admin.service;

import com.back.sportteam.domain.admin.dto.response.AdminFacilityResponse;
import com.back.sportteam.domain.admin.dto.response.AdminUserResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.exception.UserErrorCode;
import com.back.sportteam.domain.user.repository.UserRepository;
import com.back.sportteam.global.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private static final BigDecimal DEFAULT_BLACKLIST_MANNER_SCORE = new BigDecimal("2.50");
    private static final int DEFAULT_BLACKLIST_REVIEW_COUNT = 3;
    private static final String DEFAULT_RESTRICTION_REASON = "관리자에 의한 이용 제한";

    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminFacilityResponse> getFacilities(FacilityStatus status, Pageable pageable) {
        Page<Facility> facilities = status == null
                ? facilityRepository.findAll(pageable)
                : facilityRepository.findAllByStatus(status, pageable);
        return facilities.map(AdminFacilityResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(UserRole role, Pageable pageable) {
        Page<User> users = role == null
                ? userRepository.findAll(pageable)
                : userRepository.findAllByRole(role, pageable);
        return users.map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getRestrictedUsers(Pageable pageable) {
        return userRepository.findAllByRestrictedTrue(pageable)
                .map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getBlacklistCandidates(
            BigDecimal maxMannerScore,
            Integer minReviewCount,
            Pageable pageable
    ) {
        BigDecimal scoreThreshold = maxMannerScore == null ? DEFAULT_BLACKLIST_MANNER_SCORE : maxMannerScore;
        int reviewThreshold = minReviewCount == null ? DEFAULT_BLACKLIST_REVIEW_COUNT : minReviewCount;

        return userRepository
                .findAllByMannerReviewCountGreaterThanEqualAndMannerScoreLessThanEqual(
                        reviewThreshold,
                        scoreThreshold,
                        pageable
                )
                .map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse updateUserRestriction(String userId, Boolean restricted, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(restricted)) {
            user.restrict(resolveRestrictionReason(reason));
        } else {
            user.releaseRestriction();
        }

        return AdminUserResponse.from(user);
    }

    private String resolveRestrictionReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return DEFAULT_RESTRICTION_REASON;
        }
        return reason;
    }
}
