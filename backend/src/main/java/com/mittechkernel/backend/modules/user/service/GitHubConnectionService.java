package com.mittechkernel.backend.modules.user.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ServiceUnavailableException;
import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubTokenCipher;
import com.mittechkernel.backend.common.github.GitHubIdentity;
import com.mittechkernel.backend.common.github.GitHubProperties;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.user.dto.GitHubCallbackResponse;
import com.mittechkernel.backend.modules.user.dto.GitHubConnectionResponse;
import com.mittechkernel.backend.modules.user.dto.GitHubConnectionStatusResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GitHubConnectionService {

    private static final String GITHUB_PENDING_CONNECTIONS = "GITHUB_PENDING_CONNECTIONS";
    private static final int MAX_PENDING_CONNECTIONS = 5;
    private static final Duration PENDING_CONNECTION_LIFETIME = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GitHubProperties gitHubProperties;
    private final GitHubClient gitHubClient;
    private final CurrentUserService currentUserService;
    private final GitHubTokenCipher tokenCipher;
    private final JdbcTemplate jdbcTemplate;

    public GitHubConnectionService(GitHubProperties gitHubProperties,
                                  GitHubClient gitHubClient,
                                  CurrentUserService currentUserService,
                                  GitHubTokenCipher tokenCipher,
                                  JdbcTemplate jdbcTemplate) {
        this.gitHubProperties = gitHubProperties;
        this.gitHubClient = gitHubClient;
        this.currentUserService = currentUserService;
        this.tokenCipher = tokenCipher;
        this.jdbcTemplate = jdbcTemplate;
    }

    public GitHubConnectionResponse initiateConnection(HttpSession session) {
        requireOAuthConfiguration();
        CurrentUser currentUser = currentUserService.getCurrentUser();
        String state = newOAuthState();
        String codeVerifier = newOAuthState();
        String codeChallenge = createCodeChallenge(codeVerifier);
        synchronized (session) {
            Map<String, PendingConnection> pending = pendingConnections(session);
            removeExpiredPendingConnections(pending, Instant.now());
            session.setAttribute(GITHUB_PENDING_CONNECTIONS, pending);
            if (pending.size() >= MAX_PENDING_CONNECTIONS) {
                throw new BadRequestException("Too many GitHub authorization attempts are pending. Complete or close one and retry.");
            }
            pending.put(state, new PendingConnection(currentUser.id(), codeVerifier,
                    Instant.now().plus(PENDING_CONNECTION_LIFETIME)));
            session.setAttribute(GITHUB_PENDING_CONNECTIONS, pending);
        }

        String authorizationUrl = UriComponentsBuilder.fromUriString(gitHubProperties.getOauthAuthorizeUrl())
                .queryParam("client_id", gitHubProperties.getClientId())
                .queryParam("redirect_uri", gitHubProperties.getRedirectUri())
                .queryParam("scope", gitHubProperties.getScope())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();

        return new GitHubConnectionResponse("GITHUB", state, authorizationUrl);
    }

    public GitHubConnectionStatusResponse getStatus() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return jdbcTemplate.query("SELECT github_login FROM github_identities WHERE user_id = ?",
                rs -> rs.next()
                        ? new GitHubConnectionStatusResponse("GITHUB", true, rs.getString("github_login"))
                        : new GitHubConnectionStatusResponse("GITHUB", false, null), currentUser.id());
    }

    @Transactional
    public GitHubCallbackResponse validateCallback(String code, String state, String providerError, HttpSession session) {
        if (state == null || state.isBlank()) {
            throw new BadRequestException("GitHub callback state is required");
        }

        PendingConnection pendingConnection;
        synchronized (session) {
            Map<String, PendingConnection> pending = pendingConnections(session);
            removeExpiredPendingConnections(pending, Instant.now());
            session.setAttribute(GITHUB_PENDING_CONNECTIONS, pending);
            if (pending.isEmpty()) {
                session.removeAttribute(GITHUB_PENDING_CONNECTIONS);
                throw new BadRequestException("Invalid GitHub callback state");
            }
            String matchingState = pending.keySet().stream().filter(expected -> statesMatch(expected, state)).findFirst().orElse(null);
            if (matchingState == null) throw new BadRequestException("Invalid GitHub callback state");
            pendingConnection = pending.remove(matchingState);
            if (pendingConnection == null || pendingConnection.userId() == null) {
                throw new BadRequestException("GitHub connection session is invalid");
            }
            if (isBlank(pendingConnection.codeVerifier())) {
                throw new BadRequestException("GitHub PKCE session is invalid");
            }
            if (pending.isEmpty()) session.removeAttribute(GITHUB_PENDING_CONNECTIONS);
            else session.setAttribute(GITHUB_PENDING_CONNECTIONS, pending);
        }

        if (providerError != null && !providerError.isBlank()) {
            if ("access_denied".equals(providerError)) {
                throw new BadRequestException("GitHub authorization was cancelled.");
            }
            throw new BadRequestException("GitHub authorization was not completed.");
        }
        if (code == null || code.isBlank()) {
            throw new BadRequestException("GitHub authorization code is required");
        }

        String accessToken = gitHubClient.exchangeCodeForAccessToken(code, gitHubProperties.getRedirectUri(), pendingConnection.codeVerifier());
        GitHubIdentity identity = gitHubClient.getAuthenticatedUser(accessToken);
        persistConnection(pendingConnection.userId(), accessToken, identity);

        return new GitHubCallbackResponse("GITHUB", "accepted");
    }

    @Transactional(readOnly = true)
    public boolean isGitHubConnected(Long userId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM github_identities WHERE user_id = ?)", Boolean.class, userId));
    }

    @Transactional(readOnly = true)
    public String requireAccessTokenForUser(Long userId) {
        var credentials = jdbcTemplate.query("""
                        SELECT access_token_ciphertext, encryption_key_version
                        FROM github_credentials WHERE user_id = ?
                        """, rs -> rs.next()
                        ? new StoredCredential(rs.getBytes("access_token_ciphertext"), rs.getString("encryption_key_version"))
                        : null, userId);
        if (credentials == null) {
            throw new ForbiddenException("GitHub account is not connected");
        }
        return tokenCipher.decrypt(credentials.ciphertext(), credentials.keyVersion());
    }

    @Transactional
    public void connectUser(Long userId, String accessToken, GitHubIdentity identity) {
        persistConnection(userId, accessToken, identity);
    }

    @Transactional
    public void disconnectUser(Long userId) {
        jdbcTemplate.update("DELETE FROM github_credentials WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM github_identities WHERE user_id = ?", userId);
    }

    @Transactional
    public void disconnectCurrentUser() {
        disconnectUser(currentUserService.getCurrentUser().id());
    }

    private void persistConnection(Long userId, String accessToken, GitHubIdentity identity) {
        if (userId == null || identity == null || identity.id() == null || identity.login() == null || identity.login().isBlank()) {
            throw new BadRequestException("GitHub account identity is incomplete");
        }
        GitHubTokenCipher.EncryptedToken encrypted = tokenCipher.encrypt(accessToken);
        jdbcTemplate.update("""
                INSERT INTO github_identities (user_id, github_user_id, github_login, connected_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                ON CONFLICT (user_id) DO UPDATE SET
                    github_user_id = EXCLUDED.github_user_id,
                    github_login = EXCLUDED.github_login,
                    updated_at = now()
                """, userId, identity.id(), identity.login());
        jdbcTemplate.update("""
                INSERT INTO github_credentials (user_id, access_token_ciphertext, encryption_key_version, created_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                ON CONFLICT (user_id) DO UPDATE SET
                    access_token_ciphertext = EXCLUDED.access_token_ciphertext,
                    encryption_key_version = EXCLUDED.encryption_key_version,
                    updated_at = now()
                """, userId, encrypted.ciphertext(), encrypted.keyVersion());
    }

    private record StoredCredential(byte[] ciphertext, String keyVersion) {
    }

    private void requireOAuthConfiguration() {
        if (isBlank(gitHubProperties.getClientId()) || isBlank(gitHubProperties.getClientSecret())
                || isBlank(gitHubProperties.getRedirectUri()) || isBlank(gitHubProperties.getOauthAuthorizeUrl())
                || isBlank(gitHubProperties.getOauthTokenUrl()) || isBlank(gitHubProperties.getScope())) {
            throw new ServiceUnavailableException("GitHub OAuth is not configured. Set the GitHub OAuth environment variables and retry.");
        }
        try {
            tokenCipher.validateConfiguration();
        } catch (IllegalStateException ex) {
            throw new ServiceUnavailableException("GitHub token encryption is not configured correctly.");
        }
    }

    private String newOAuthState() {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String createCodeChallenge(String verifier) {
        try {
            byte[] challenge = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private boolean statesMatch(String expectedState, String actualState) {
        if (expectedState == null || actualState == null) {
            return false;
        }
        return MessageDigest.isEqual(expectedState.getBytes(StandardCharsets.UTF_8), actualState.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private Map<String, PendingConnection> pendingConnections(HttpSession session) {
        Object value = session.getAttribute(GITHUB_PENDING_CONNECTIONS);
        if (value == null) return new LinkedHashMap<>();
        if (value instanceof Map<?, ?>) return (Map<String, PendingConnection>) value;
        throw new BadRequestException("GitHub connection session is invalid");
    }

    private void removeExpiredPendingConnections(Map<String, PendingConnection> pending, Instant now) {
        pending.entrySet().removeIf(entry -> entry.getValue() == null
                || entry.getValue().expiresAt() == null
                || !entry.getValue().expiresAt().isAfter(now));
    }

    record PendingConnection(Long userId, String codeVerifier, Instant expiresAt) implements java.io.Serializable {}

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
