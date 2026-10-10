package com.mittechkernel.backend.modules.auth.repository;

import com.mittechkernel.backend.modules.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findByIdAndActiveTrue(Long id);
}
