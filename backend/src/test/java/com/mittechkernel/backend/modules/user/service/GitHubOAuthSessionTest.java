package com.mittechkernel.backend.modules.user.service;

import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubProperties;
import com.mittechkernel.backend.common.github.GitHubTokenCipher;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubOAuthSessionTest {
    private final GitHubProperties properties = properties();
    private final GitHubConnectionService service = new GitHubConnectionService(properties,
            unusedGitHubClient(), new CurrentUserService(), new GitHubTokenCipher(properties), new JdbcTemplate());

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void stateAndPkceSessionSurviveInitiationAndCallbackAndAreOneTime() {
        authenticate(42L);
        HttpSession session = new MockHttpSession();
        var initiated = service.initiateConnection(session);

        assertThat(initiated.authorizationUrl()).contains("state=" + initiated.state(), "code_challenge_method=S256");
        assertThatThrownBy(() -> service.validateCallback(null, initiated.state(), "access_denied", session))
                .hasMessage("GitHub authorization was cancelled.");
        assertThatThrownBy(() -> service.validateCallback(null, initiated.state(), "access_denied", session))
                .hasMessage("Invalid GitHub callback state");
    }

    @Test
    void stateMismatchDoesNotConsumeAnotherPendingFlowAndMultipleAttemptsDoNotOverwrite() {
        authenticate(42L);
        HttpSession session = new MockHttpSession();
        var first = service.initiateConnection(session);
        var second = service.initiateConnection(session);

        assertThatThrownBy(() -> service.validateCallback(null, "invalid-state", "access_denied", session))
                .hasMessage("Invalid GitHub callback state");
        assertThatThrownBy(() -> service.validateCallback(null, first.state(), "access_denied", session))
                .hasMessage("GitHub authorization was cancelled.");
        assertThatThrownBy(() -> service.validateCallback(null, second.state(), "access_denied", session))
                .hasMessage("GitHub authorization was cancelled.");
    }

    @Test
    void expiredPendingStatesAreRejectedAndRemoved() {
        authenticate(42L);
        HttpSession session = new MockHttpSession();
        var initiated = service.initiateConnection(session);
        @SuppressWarnings("unchecked")
        Map<String, GitHubConnectionService.PendingConnection> pending =
                (Map<String, GitHubConnectionService.PendingConnection>) session.getAttribute("GITHUB_PENDING_CONNECTIONS");
        pending.put(initiated.state(), new GitHubConnectionService.PendingConnection(42L, "test-verifier", Instant.now().minusSeconds(1)));

        assertThatThrownBy(() -> service.validateCallback(null, initiated.state(), "access_denied", session))
                .hasMessage("Invalid GitHub callback state");
        assertThat(session.getAttribute("GITHUB_PENDING_CONNECTIONS")).isNull();
    }

    private void authenticate(long userId) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new CurrentUser(userId, "member@example.com", Set.of("STUDENT")), "unused", List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static GitHubProperties properties() {
        var value = new GitHubProperties();
        value.setClientId("test-client");
        value.setClientSecret("test-secret");
        value.setTokenEncryptionKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return value;
    }

    private static GitHubClient unusedGitHubClient() {
        return new GitHubClient() {
            public String exchangeCodeForAccessToken(String code, String redirectUri, String codeVerifier) { throw new AssertionError(); }
            public com.mittechkernel.backend.common.github.GitHubIdentity getAuthenticatedUser(String accessToken) { throw new AssertionError(); }
            public List<com.mittechkernel.backend.common.github.GitHubRepository> listAccessibleRepositories(String accessToken) { throw new AssertionError(); }
            public com.mittechkernel.backend.common.github.GitHubRepository getRepository(String accessToken, Long repositoryId) { throw new AssertionError(); }
            public List<com.mittechkernel.backend.common.github.GitHubCommit> listCommits(String accessToken, String owner, String repository) { throw new AssertionError(); }
        };
    }
}
