package com.mittechkernel.backend.modules.auth.repository;

import com.mittechkernel.backend.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);

    @Modifying
    @Query("delete from RefreshToken token where token.id = :id")
    int deleteIfPresentById(@Param("id") Long id);
}
