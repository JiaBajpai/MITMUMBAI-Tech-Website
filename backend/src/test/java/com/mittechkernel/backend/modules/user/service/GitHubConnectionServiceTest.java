package com.mittechkernel.backend.modules.user.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ServiceUnavailableException;
import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubCommit;
import com.mittechkernel.backend.common.github.GitHubIdentity;
import com.mittechkernel.backend.common.github.GitHubProperties;
import com.mittechkernel.backend.common.github.GitHubRepository;
import com.mittechkernel.backend.common.github.GitHubTokenCipher;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubConnectionServiceTest {

    private GitHubProperties properties;
    private GitHubClient gitHubClient;
    private CurrentUserService currentUserService;
    private GitHubConnectionService service;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        properties = configuredProperties();
        gitHubClient = new TestGitHubClient();
        currentUserService = new CurrentUserService();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new CurrentUser(47L, "member@example.test", Set.of("SUPER_ADMIN")), null, List.of()));
        service = new GitHubConnectionService(properties, gitHubClient, currentUserService,
                new GitHubTokenCipher(properties), new RecordingJdbcTemplate());
        session = new MockHttpSession();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildsGitHubAuthorizationUrlWithConfiguredClientAndCallbackAndStrongState() {
        var result = service.initiateConnection(session);
        var query = UriComponentsBuilder.fromUriString(result.authorizationUrl()).build().getQueryParams();

        assertThat(result.authorizationUrl()).startsWith("https://github.com/login/oauth/authorize?");
        assertThat(query.getFirst("client_id")).isEqualTo("registered-oauth-client");
        assertThat(query.getFirst("redirect_uri")).isEqualTo("http://localhost:8080/api/v1/github/oauth/callback");
        assertThat(query.getFirst("scope")).isEqualTo("read:user");
        assertThat(query.getFirst("state")).isEqualTo(result.state());
        assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
        assertThat(query.getFirst("code_challenge")).matches("[A-Za-z0-9_-]{43}");
        assertThat(query.getFirst("code_challenge")).isNotEqualTo(session.getAttribute("GITHUB_PKCE_VERIFIER"));
        assertThat(result.state()).matches("[A-Za-z0-9_-]{43}");
        assertThat(result.authorizationUrl()).doesNotContain("client_secret", "registered-oauth-secret");
        assertThat(session.getAttribute("GITHUB_OAUTH_STATE")).isEqualTo(result.state());
    }

    @Test
    void refusesToStartOAuthWhenClientConfigurationIsMissing() {
        properties.setClientId("");

        assertThatThrownBy(() -> service.initiateConnection(session))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("GitHub OAuth is not configured");
        assertThat(session.getAttribute("GITHUB_OAUTH_STATE")).isNull();
        assertThat(((TestGitHubClient) gitHubClient).exchangeCount).isZero();
    }

    @Test
    void refusesToStartOAuthWhenTokenEncryptionKeyIsMissing() {
        properties.setTokenEncryptionKey("");

        assertThatThrownBy(() -> service.initiateConnection(session))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("token encryption is not configured");
        assertThat(session.getAttribute("GITHUB_OAUTH_STATE")).isNull();
    }

    @Test
    void rejectsMismatchedStateBeforeExchangingAuthorizationCode() {
        session.setAttribute("GITHUB_OAUTH_STATE", "expected-state");
        session.setAttribute("GITHUB_PENDING_USER_ID", 47L);

        assertThatThrownBy(() -> service.validateCallback("one-time-code", "attacker-state", null, session))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid GitHub callback state");
        assertThat(((TestGitHubClient) gitHubClient).exchangeCount).isZero();
    }

    @Test
    void validatesCallbackPersistsConnectionAndConsumesStateForReplayProtection() {
        session.setAttribute("GITHUB_OAUTH_STATE", "valid-state");
        session.setAttribute("GITHUB_PENDING_USER_ID", 47L);
        session.setAttribute("GITHUB_PKCE_VERIFIER", "valid-code-verifier");
        TestGitHubClient testClient = (TestGitHubClient) gitHubClient;

        var response = service.validateCallback("one-time-code", "valid-state", null, session);

        assertThat(response.status()).isEqualTo("accepted");
        assertThat(session.getAttribute("GITHUB_OAUTH_STATE")).isNull();
        assertThat(session.getAttribute("GITHUB_PENDING_USER_ID")).isNull();
        assertThat(session.getAttribute("GITHUB_PKCE_VERIFIER")).isNull();
        assertThat(testClient.exchangeCount).isEqualTo(1);
        assertThat(testClient.lastCodeVerifier).isEqualTo("valid-code-verifier");

        assertThatThrownBy(() -> service.validateCallback("one-time-code", "valid-state", null, session))
                .isInstanceOf(BadRequestException.class);
        assertThat(testClient.exchangeCount).isEqualTo(1);
    }

    @Test
    void validatesStateAndReportsUserCancellationWithoutExchangingCode() {
        session.setAttribute("GITHUB_OAUTH_STATE", "valid-state");
        session.setAttribute("GITHUB_PENDING_USER_ID", 47L);
        session.setAttribute("GITHUB_PKCE_VERIFIER", "valid-code-verifier");

        assertThatThrownBy(() -> service.validateCallback(null, "valid-state", "access_denied", session))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("authorization was cancelled");
        assertThat(session.getAttribute("GITHUB_OAUTH_STATE")).isNull();
        assertThat(((TestGitHubClient) gitHubClient).exchangeCount).isZero();
    }

    private GitHubProperties configuredProperties() {
        GitHubProperties configured = new GitHubProperties();
        configured.setClientId("registered-oauth-client");
        configured.setClientSecret("registered-oauth-secret");
        configured.setRedirectUri("http://localhost:8080/api/v1/github/oauth/callback");
        configured.setScope("read:user");
        configured.setOauthAuthorizeUrl("https://github.com/login/oauth/authorize");
        configured.setOauthTokenUrl("https://github.com/login/oauth/access_token");
        configured.setTokenEncryptionKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return configured;
    }

    private static final class TestGitHubClient implements GitHubClient {
        private int exchangeCount;
        private String lastCodeVerifier;

        @Override
        public String exchangeCodeForAccessToken(String code, String redirectUri, String codeVerifier) {
            exchangeCount++;
            lastCodeVerifier = codeVerifier;
            return "never-return-this-token";
        }

        @Override
        public GitHubIdentity getAuthenticatedUser(String accessToken) {
            return new GitHubIdentity(123L, "kernel-member", "Kernel Member", null, null,
                    "https://github.com/kernel-member");
        }

        @Override
        public List<GitHubRepository> listAccessibleRepositories(String accessToken) {
            return List.of();
        }

        @Override
        public GitHubRepository getRepository(String accessToken, Long repositoryId) {
            return null;
        }

        @Override
        public List<GitHubCommit> listCommits(String accessToken, String owner, String repository) {
            return List.of();
        }
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        @Override
        public int update(String sql, Object... args) {
            return 1;
        }
    }
}
