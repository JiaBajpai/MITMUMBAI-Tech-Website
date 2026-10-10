package com.mittechkernel.backend.modules.auth.service;

import com.mittechkernel.backend.common.exception.ExpiredTokenException;
import com.mittechkernel.backend.common.exception.InvalidCredentialsException;
import com.mittechkernel.backend.common.exception.InvalidTokenException;
import com.mittechkernel.backend.modules.auth.dto.AuthSuccessResponse;
import com.mittechkernel.backend.modules.auth.dto.UserPublicResponse;
import com.mittechkernel.backend.modules.auth.entity.AuthUser;
import com.mittechkernel.backend.modules.auth.entity.RefreshToken;
import com.mittechkernel.backend.modules.auth.repository.AuthUserRepository;
import com.mittechkernel.backend.modules.auth.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthUserRepository authUserRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.authUserRepository = authUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthSuccessResponse login(String email, String password) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles(), user.getAuthVersion());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(new RefreshToken(user, hashToken(refreshToken), Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs())));

        return new AuthSuccessResponse(accessToken, refreshToken, toPublicResponse(user));
    }

    @Transactional
    public AuthSuccessResponse refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        jwtService.validateRefreshToken(refreshTokenValue);
        String tokenHash = hashToken(refreshTokenValue);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or revoked"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new ExpiredTokenException("Refresh token expired");
        }

        AuthUser user = authUserRepository.findById(storedToken.getUser().getId())
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

        if (!user.isActive()) {
            throw new InvalidTokenException("User is no longer active");
        }

        if (refreshTokenRepository.deleteIfPresentById(storedToken.getId()) != 1) {
            throw new InvalidTokenException("Refresh token is invalid or already used");
        }
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles(), user.getAuthVersion());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenRepository.save(new RefreshToken(user, hashToken(newRefreshToken), Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs())));

        return new AuthSuccessResponse(newAccessToken, newRefreshToken, toPublicResponse(user));
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        refreshTokenRepository.findByTokenHash(hashToken(refreshTokenValue))
                .ifPresent(refreshTokenRepository::delete);
    }

    public UserPublicResponse getCurrentUserProfile(AuthUser user) {
        return toPublicResponse(user);
    }

    private UserPublicResponse toPublicResponse(AuthUser user) {
        return new UserPublicResponse(user.getId(), user.getName(), user.getEmail(), user.getProgram(), user.getRoles());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
