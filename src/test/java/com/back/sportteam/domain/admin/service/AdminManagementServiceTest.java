package com.back.sportteam.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.admin.dto.response.AdminFacilityResponse;
import com.back.sportteam.domain.admin.dto.response.AdminUserResponse;
import com.back.sportteam.domain.facility.entity.Facility;
import com.back.sportteam.domain.facility.entity.FacilityDetails;
import com.back.sportteam.domain.facility.entity.FacilityStatus;
import com.back.sportteam.domain.facility.repository.FacilityRepository;
import com.back.sportteam.domain.match.entity.SportType;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import com.back.sportteam.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminManagementService adminManagementService;

    @Test
    void 관리자는_플랫폼의_전체_시설을_조회한다() {
        Facility facility = createFacility();
        Pageable pageable = PageRequest.of(0, 20);
        when(facilityRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(facility), pageable, 1));

        Page<AdminFacilityResponse> response = adminManagementService.getFacilities(null, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().facilityId()).isEqualTo(facility.getId());
        assertThat(response.getContent().getFirst().managerId()).isEqualTo("manager-id");
    }

    @Test
    void 관리자는_상태별로_시설을_조회한다() {
        Pageable pageable = PageRequest.of(0, 20);
        when(facilityRepository.findAllByStatus(FacilityStatus.ACTIVE, pageable))
                .thenReturn(Page.empty(pageable));

        adminManagementService.getFacilities(FacilityStatus.ACTIVE, pageable);

        verify(facilityRepository).findAllByStatus(FacilityStatus.ACTIVE, pageable);
    }

    @Test
    void 관리자는_플랫폼의_전체_회원을_조회한다() {
        User user = User.local("user@example.com", "사용자", "password-hash", UserRole.USER);
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        Page<AdminUserResponse> response = adminManagementService.getUsers(null, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().email()).isEqualTo("user@example.com");
        assertThat(response.getContent().getFirst().role()).isEqualTo(UserRole.USER);
    }

    @Test
    void 관리자는_역할별로_회원을_조회한다() {
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAllByRole(UserRole.MANAGER, pageable))
                .thenReturn(Page.empty(pageable));

        adminManagementService.getUsers(UserRole.MANAGER, pageable);

        verify(userRepository).findAllByRole(UserRole.MANAGER, pageable);
    }

    private Facility createFacility() {
        return Facility.create(
                "manager-id",
                "테스트 시설",
                "서울시 강남구",
                FacilityDetails.builder()
                        .capacity(10)
                        .slotDurationMinutes(60)
                        .defaultWeekdayPrice(50_000)
                        .defaultWeekendPrice(70_000)
                        .sportTypes(Set.of(SportType.FUTSAL))
                        .build()
        );
    }
}
