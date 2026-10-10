package com.mittechkernel.backend.modules.auth.service;

import com.mittechkernel.backend.common.exception.ExpiredTokenException;
import com.mittechkernel.backend.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                     @Value("${app.security.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
                     @Value("${app.security.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(Long userId, String email) {
        return generateAccessToken(userId, email, Set.of());
    }

    public String generateAccessToken(Long userId, String email, Set<String> roles) {
        return generateAccessToken(userId, email, roles, 0);
    }

    public String generateAccessToken(Long userId, String email, Set<String> roles, int authVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("authVersion", authVersion)
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("userId", userId)
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenExpirationMs)))
                .signWith(secretKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!"ACCESS".equals(claims.get("tokenType", String.class))) {
            throw new InvalidTokenException("Access token is required");
        }
        if (claims.getExpiration().before(new Date())) {
            throw new ExpiredTokenException("Access token expired");
        }
    }

    public void validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!"REFRESH".equals(claims.get("tokenType", String.class))) {
            throw new InvalidTokenException("Refresh token is required");
        }
        if (claims.getExpiration().before(new Date())) {
            throw new ExpiredTokenException("Refresh token expired");
        }
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public int extractAuthVersion(String token) {
        Integer version = parseClaims(token).get("authVersion", Integer.class);
        return version == null ? 0 : version;
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Set<String> extractRoles(String token) {
        List<?> roles = parseClaims(token).get("roles", List.class);
        if (roles == null) {
            return Set.of();
        }
        return Set.copyOf(roles.stream().map(String::valueOf).toList());
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(parseClaims(token).get("tokenType", String.class));
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new InvalidTokenException("Invalid token");
        }
    }
}
