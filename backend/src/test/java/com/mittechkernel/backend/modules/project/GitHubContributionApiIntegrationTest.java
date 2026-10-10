package com.mittechkernel.backend.modules.project;

import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubCommit;
import com.mittechkernel.backend.common.github.GitHubIdentity;
import com.mittechkernel.backend.common.github.GitHubProperties;
import com.mittechkernel.backend.common.github.GitHubRepository;
import com.mittechkernel.backend.common.github.GitHubTokenCipher;
import com.mittechkernel.backend.modules.project.repository.ProjectRepository;
import com.mittechkernel.backend.modules.gamification.XpRepository;
import com.mittechkernel.backend.modules.user.service.GitHubConnectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.github.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class GitHubContributionApiIntegrationTest {

    private static final String SHA_A = "a".repeat(40);
    private static final String SHA_B = "b".repeat(40);

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private GitHubConnectionService connectionService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private StubGitHubClient gitHubClient;
    @Autowired private XpRepository xpRepository;

    private MockMvc mockMvc;
    private Long studentId;
    private Long kernelProjectId;
    private Long foundationUserId;
    private String foundationEmail;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        studentId = currentUserId(login("student@kernel.ac.in"));
        jdbcTemplate.update("DELETE FROM projects WHERE name = ?", "Repo Link Test Project");
        jdbcTemplate.update("DELETE FROM projects WHERE name LIKE ?", "Contribution test %");
        connectionService.disconnectUser(studentId);
        gitHubClient.reset();
    }

    @AfterEach
    void cleanUp() {
        if (kernelProjectId != null) {
            projectRepository.deleteById(kernelProjectId);
            kernelProjectId = null;
        }
        if (foundationUserId != null) {
            connectionService.disconnectUser(foundationUserId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", foundationUserId);
            foundationUserId = null;
            foundationEmail = null;
        }
        connectionService.disconnectUser(studentId);
    }

    @Test
    void oauthPersistsNumericIdentityAndReconnectUpdatesWithoutDuplicates() throws Exception {
        String token = login("student@kernel.ac.in");
        authorizeGitHub(token);
        authorizeGitHub(token);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM github_identities WHERE user_id = ?", Integer.class, studentId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT github_user_id FROM github_identities WHERE user_id = ?", Long.class, studentId)).isEqualTo(99L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT github_login FROM github_identities WHERE user_id = ?", String.class, studentId)).isEqualTo("kernel-user");
    }

    @Test
    void credentialsAreEncryptedAndNeverReturnedByStatusApi() throws Exception {
        String token = login("student@kernel.ac.in");
        connectionService.connectUser(studentId, "stub-access-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));

        byte[] encrypted = jdbcTemplate.queryForObject(
                "SELECT access_token_ciphertext FROM github_credentials WHERE user_id = ?", byte[].class, studentId);
        assertThat(new String(encrypted, java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("stub-access-token");
        assertThat(encrypted).hasSizeGreaterThan(12 + "stub-access-token".length());

        String response = mockMvc.perform(get("/api/v1/me/github/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("stub-access-token", "ciphertext", "encryption_key");
    }

    @Test
    void missingEncryptionKeyFailsSafely() {
        GitHubProperties properties = new GitHubProperties();
        properties.setTokenEncryptionKey("");
        assertThatThrownBy(() -> new GitHubTokenCipher(properties).encrypt("token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GITHUB_TOKEN_ENCRYPTION_KEY");
    }

    @Test
    void disconnectRemovesIdentityAndUsableCredential() throws Exception {
        connectionService.connectUser(studentId, "stub-access-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        connectionService.disconnectUser(studentId);

        assertThat(connectionService.isGitHubConnected(studentId)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM github_credentials WHERE user_id = ?", Integer.class, studentId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM github_identities WHERE user_id = ?", Integer.class, studentId)).isZero();
    }

    @Test
    void authenticatedUserCanDisconnectThroughApi() throws Exception {
        String token = login("student@kernel.ac.in");
        connectionService.connectUser(studentId, "stub-access-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        mockMvc.perform(delete("/api/v1/me/github/connect")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/me/github/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(false));
    }

    @Test
    void retrievalPersistsAndResolvesAuthorsAndRepeatedSyncIsIdempotent() throws Exception {
        String token = connectAndLink(1234L);
        String first = retrieve(token, kernelProjectId);
        assertThat(objectMapper.readTree(first).path("data").size()).isEqualTo(2);
        assertThat(first).contains(SHA_A, "kernel-user", "outsider").doesNotContain("stub-access-token", "ciphertext");
        assertThat(gitHubClient.lastAccessToken).isEqualTo("stub-access-token");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT user_id FROM github_contributions WHERE project_id = ? AND commit_sha = ?
                """, Long.class, kernelProjectId, SHA_A)).isEqualTo(studentId);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT user_id FROM github_contributions WHERE project_id = ? AND commit_sha = ?
                """, Long.class, kernelProjectId, SHA_B)).isNull();

        retrieve(token, kernelProjectId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM github_contributions WHERE project_id = ?", Integer.class, kernelProjectId)).isEqualTo(2);
        Long importedButUnverifiedId = jdbcTemplate.queryForObject(
                "SELECT id FROM github_contributions WHERE project_id = ? AND commit_sha = ?", Long.class, kernelProjectId, SHA_A);
        assertThat(xpRepository.awardVerifiedContribution(importedButUnverifiedId)).isFalse();
    }

    @Test
    void activeDomainLeadCanVerifyResolvedActiveMembersContribution() throws Exception {
        String leadToken = login("lead.backend@kernel.ac.in");
        Long leadId = currentUserId(leadToken);
        connectionService.connectUser(leadId, "lead-stub-token",
                new GitHubIdentity(123L, "backend-lead", null, null, null, null));
        kernelProjectId = createProjectInBackendDomain(leadToken);
        linkRepository(leadToken, kernelProjectId, 1234L);
        connectionService.connectUser(studentId, "student-stub-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        jdbcTemplate.update("INSERT INTO project_members (project_id, user_id, role, status, created_at, updated_at) VALUES (?, ?, 'MEMBER', 'ACTIVE', now(), now()) ON CONFLICT (project_id, user_id) DO UPDATE SET status = 'ACTIVE'",
                kernelProjectId, studentId);
        retrieve(leadToken, kernelProjectId);
        Long contributionId = jdbcTemplate.queryForObject(
                "SELECT id FROM github_contributions WHERE project_id = ? AND commit_sha = ?",
                Long.class, kernelProjectId, SHA_A);

        mockMvc.perform(post("/api/v1/projects/{projectId}/github/contributions/{contributionId}/verify",
                        kernelProjectId, contributionId)
                        .header("Authorization", "Bearer " + leadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.verifiedAt").isNotEmpty());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verified_by FROM github_contributions WHERE id = ?", Long.class, contributionId)).isEqualTo(leadId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT amount FROM xp_events WHERE source = 'CONTRIBUTION' AND ref_type = 'GITHUB_CONTRIBUTION' AND ref_id = ?",
                Integer.class, contributionId)).isEqualTo(40);
        assertThat(xpRepository.awardVerifiedContribution(contributionId)).isFalse();
        mockMvc.perform(delete("/api/v1/projects/{id}/github/repository", kernelProjectId)
                        .header("Authorization", "Bearer " + leadToken))
                .andExpect(status().isBadRequest());

        long leaderboardXpBeforeDisconnect = leaderboardXpFor(studentId, leadToken);
        connectionService.disconnectUser(studentId);
        foundationUserId = createFoundationUser();
        connectionService.connectUser(foundationUserId, "foundation-stub-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        retrieve(leadToken, kernelProjectId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM github_contributions WHERE id = ?", Long.class, contributionId)).isEqualTo(studentId);
        assertThat(leaderboardXpFor(studentId, leadToken)).isEqualTo(leaderboardXpBeforeDisconnect);
    }

    private Long createProjectInBackendDomain(String token) throws Exception {
        Long domainId = jdbcTemplate.queryForObject("SELECT id FROM domains WHERE name = 'Backend'", Long.class);
        String body = "{\"name\":\"Contribution verify " + UUID.randomUUID()
                + "\",\"domainId\":" + domainId
                + ",\"program\":\"TECHNICAL\",\"problem\":\"test\",\"solution\":\"test\","
                + "\"technologies\":[\"Java\"],\"requiredSkills\":[\"Spring\"],\"expectedMembers\":2}";
        String created = mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).path("data").path("id").asLong();
        kernelProjectId = id;
        return id;
    }

    @Test
    void sameCommitShaInDifferentRepositoriesDoesNotCollide() throws Exception {
        String token = login("student@kernel.ac.in");
        connectionService.connectUser(studentId, "stub-access-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        Long firstProject = createProject(token);
        linkRepository(token, firstProject, 1234L);
        retrieve(token, firstProject);

        Long secondProject = createProject(token);
        linkRepository(token, secondProject, 4321L);
        retrieve(token, secondProject);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM github_contributions WHERE commit_sha = ?", Integer.class, SHA_A)).isEqualTo(2);
        projectRepository.deleteById(firstProject);
    }

    @Test
    void unauthenticatedAndUnauthorizedProjectAccessAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/projects/1/github/contributions"))
                .andExpect(status().isUnauthorized());

        String ownerToken = login("student@kernel.ac.in");
        connectAndLink(1234L);
        String outsiderToken = login("faculty@kernel.ac.in");
        mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", kernelProjectId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyActiveMembersCanReadLinkedRepositoryOrSyncContributions() throws Exception {
        String ownerToken = connectAndLink(1234L);
        String memberToken = login("faculty@kernel.ac.in");
        Long memberId = currentUserId(memberToken);
        mockMvc.perform(post("/api/v1/projects/{id}/join", kernelProjectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/{id}/github/repository", kernelProjectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", kernelProjectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        assertThat(gitHubClient.lastAccessToken).isNull();

        jdbcTemplate.update("UPDATE project_members SET status = 'REJECTED' WHERE project_id = ? AND user_id = ?",
                kernelProjectId, memberId);
        mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", kernelProjectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        assertThat(gitHubClient.lastAccessToken).isNull();

        jdbcTemplate.update("UPDATE project_members SET status = 'ACTIVE' WHERE project_id = ? AND user_id = ?",
                kernelProjectId, memberId);
        mockMvc.perform(get("/api/v1/projects/{id}/github/repository", kernelProjectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", kernelProjectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
        assertThat(gitHubClient.lastAccessToken).isEqualTo("stub-access-token");

        String coreToken = login("core@kernel.ac.in");
        mockMvc.perform(get("/api/v1/projects/{id}/github/repository", kernelProjectId)
                        .header("Authorization", "Bearer " + coreToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/{id}/github/repository", kernelProjectId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void projectCreationAndMembershipRejectCrossProgramUsers() throws Exception {
        String technicalToken = login("student@kernel.ac.in");
        Long domainId = jdbcTemplate.queryForObject("SELECT id FROM domains WHERE name = 'Backend'", Long.class);
        String incompatibleProject = projectBody("Mismatched Foundation Project", domainId, "FOUNDATION");
        mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + technicalToken)
                        .contentType(MediaType.APPLICATION_JSON).content(incompatibleProject))
                .andExpect(status().isForbidden());

        String coreToken = login("core@kernel.ac.in");
        mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(incompatibleProject))
                .andExpect(status().isForbidden());

        foundationUserId = createFoundationUser();
        String foundationToken = login(foundationEmail);
        Long foundationProjectId = createProject(foundationToken, "FOUNDATION");
        mockMvc.perform(post("/api/v1/projects/{id}/join", foundationProjectId)
                        .header("Authorization", "Bearer " + technicalToken))
                .andExpect(status().isForbidden());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM project_members WHERE project_id = ? AND user_id = ?",
                Integer.class, foundationProjectId, studentId)).isZero();
    }

    @Test
    void contributionVerificationRejectsCrossProgramAuthorButAllowsSameProgramFlow() throws Exception {
        foundationUserId = createFoundationUser();
        String foundationToken = login(foundationEmail);
        connectionService.connectUser(foundationUserId, "foundation-stub-token",
                new GitHubIdentity(88L, "foundation-owner", null, null, null, null));
        kernelProjectId = createProject(foundationToken, "FOUNDATION");
        linkRepository(foundationToken, kernelProjectId, 1234L);
        connectionService.connectUser(studentId, "student-stub-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        jdbcTemplate.update("INSERT INTO project_members (project_id, user_id, role, status, created_at, updated_at) VALUES (?, ?, 'MEMBER', 'ACTIVE', now(), now())",
                kernelProjectId, studentId);
        retrieve(foundationToken, kernelProjectId);
        Long contributionId = jdbcTemplate.queryForObject(
                "SELECT id FROM github_contributions WHERE project_id = ? AND commit_sha = ?",
                Long.class, kernelProjectId, SHA_A);

        String coreToken = login("core@kernel.ac.in");
        mockMvc.perform(post("/api/v1/projects/{projectId}/github/contributions/{contributionId}/verify",
                        kernelProjectId, contributionId)
                        .header("Authorization", "Bearer " + coreToken))
                .andExpect(status().isForbidden());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT verified FROM github_contributions WHERE id = ?", Boolean.class, contributionId)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM xp_events WHERE source = 'CONTRIBUTION' AND ref_id = ?",
                Integer.class, contributionId)).isZero();
    }

    @Test
    void missingProjectRepositoryAndCredentialsAreReported() throws Exception {
        String token = login("student@kernel.ac.in");
        kernelProjectId = createProject(token);
        mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", kernelProjectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        connectionService.connectUser(studentId, "stub-access-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        linkRepository(token, kernelProjectId, 1234L);
        connectionService.disconnectUser(studentId);
        mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", kernelProjectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void githubClientFailureDoesNotCreateContributionRows() throws Exception {
        String token = connectAndLink(1234L);
        gitHubClient.failCommits = true;
        mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", kernelProjectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadGateway());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM github_contributions WHERE project_id = ?", Integer.class, kernelProjectId)).isZero();
    }

    private String connectAndLink(Long repositoryId) throws Exception {
        String token = login("student@kernel.ac.in");
        connectionService.connectUser(studentId, "stub-access-token",
                new GitHubIdentity(99L, "kernel-user", null, null, null, null));
        kernelProjectId = createProject(token);
        linkRepository(token, kernelProjectId, repositoryId);
        return token;
    }

    private void authorizeGitHub(String kernelToken) throws Exception {
        MvcResult initiated = mockMvc.perform(post("/api/v1/me/github/connect")
                        .header("Authorization", "Bearer " + kernelToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode response = objectMapper.readTree(initiated.getResponse().getContentAsString());
        String state = response.path("data").path("state").asText();
        MockHttpSession session = (MockHttpSession) initiated.getRequest().getSession(false);
        mockMvc.perform(get("/api/v1/github/oauth/callback")
                        .session(session).param("code", "authorization-code").param("state", state))
                .andExpect(status().isOk());
    }

    private String retrieve(String token, Long projectId) throws Exception {
        return mockMvc.perform(get("/api/v1/projects/{id}/github/contributions", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long leaderboardXpFor(Long userId, String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/leaderboard").param("program", "TECHNICAL").param("size", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode entries = objectMapper.readTree(response).path("data").path("entries");
        for (JsonNode entry : entries) {
            if (entry.path("userId").asLong() == userId) return entry.path("totalXp").asLong();
        }
        throw new AssertionError("Expected user to appear in the technical leaderboard");
    }

    private void linkRepository(String token, Long projectId, Long repositoryId) throws Exception {
        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":" + repositoryId + "}"))
                .andExpect(status().isCreated());
    }

    private Long createProject(String token) throws Exception {
        return createProject(token, "TECHNICAL");
    }

    private Long createProject(String token, String program) throws Exception {
        String domains = mockMvc.perform(get("/api/v1/domains").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long domainId = objectMapper.readTree(domains).path("data").get(0).path("id").asLong();
        String name = "Contribution test " + UUID.randomUUID();
        String body = projectBody(name, domainId, program);
        String created = mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).path("data").path("id").asLong();
        kernelProjectId = id;
        return id;
    }

    private String projectBody(String name, Long domainId, String program) {
        return "{\"name\":\"" + name + "\",\"domainId\":" + domainId
                + ",\"program\":\"" + program + "\",\"problem\":\"test\",\"solution\":\"test\","
                + "\"technologies\":[\"Java\"],\"requiredSkills\":[\"Spring\"],\"expectedMembers\":2}";
    }

    private Long createFoundationUser() {
        foundationEmail = "foundation-" + UUID.randomUUID() + "@example.test";
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO users (email, name, password_hash, program, active)
                VALUES (?, 'Foundation Integration User',
                        '$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6', 'FOUNDATION', TRUE)
                RETURNING id
                """, Long.class, foundationEmail);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'STUDENT')", id);
        return id;
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").header("Origin", "http://localhost:5173").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Kernel@123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private Long currentUserId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary StubGitHubClient stubGitHubClient() { return new StubGitHubClient(); }
    }

    static class StubGitHubClient implements GitHubClient {
        volatile boolean failCommits;
        volatile String lastAccessToken;

        void reset() { failCommits = false; lastAccessToken = null; }
        @Override public String exchangeCodeForAccessToken(String code, String redirectUri, String codeVerifier) { return "oauth-stub-token"; }
        @Override public GitHubIdentity getAuthenticatedUser(String accessToken) {
            return new GitHubIdentity(99L, "kernel-user", "Kernel User", null, null, null);
        }
        @Override public List<GitHubRepository> listAccessibleRepositories(String accessToken) {
            return List.of(
                    new GitHubRepository(1234L, "kernel-org", "alpha-repo", "kernel-org/alpha-repo", "https://github.com/kernel-org/alpha-repo", "PUBLIC"),
                    new GitHubRepository(4321L, "kernel-org", "beta-repo", "kernel-org/beta-repo", "https://github.com/kernel-org/beta-repo", "PRIVATE"));
        }
        @Override public GitHubRepository getRepository(String accessToken, Long repositoryId) {
            return listAccessibleRepositories(accessToken).stream().filter(repo -> repo.id().equals(repositoryId)).findFirst().orElse(null);
        }
        @Override public List<GitHubCommit> listCommits(String accessToken, String owner, String repository) {
            lastAccessToken = accessToken;
            if (failCommits) throw new IllegalStateException("GitHub commit lookup failed");
            return List.of(
                    new GitHubCommit(SHA_A, "https://github.com/kernel-org/" + repository + "/commit/" + SHA_A,
                            "Known author commit", Instant.parse("2026-10-01T10:00:00Z"), 99L, "kernel-user"),
                    new GitHubCommit(SHA_B, "https://github.com/kernel-org/" + repository + "/commit/" + SHA_B,
                            "Unknown author commit", Instant.parse("2026-10-02T10:00:00Z"), 777L, "outsider"));
        }
    }
}
