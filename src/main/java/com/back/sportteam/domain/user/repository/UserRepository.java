package com.back.sportteam.domain.user.repository;

import com.back.sportteam.domain.auth.provider.AuthProvider;
import com.back.sportteam.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}