package com.back.sportteam.domain.user.repository;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.entity.User;
import com.back.sportteam.domain.user.entity.UserRole;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Page<User> findAllByRole(UserRole role, Pageable pageable);

    Page<User> findAllByRestrictedTrue(Pageable pageable);

    Page<User> findAllByMannerReviewCountGreaterThanEqualAndMannerScoreLessThanEqual(
            int minReviewCount,
            BigDecimal maxMannerScore,
            Pageable pageable
    );
}
