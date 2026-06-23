package com.back.sportteam.domain.admin.service;

import com.back.sportteam.domain.admin.dto.response.AdminFacilityResponse;
import com.back.sportteam.domain.admin.dto.response.AdminUserResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

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
}
