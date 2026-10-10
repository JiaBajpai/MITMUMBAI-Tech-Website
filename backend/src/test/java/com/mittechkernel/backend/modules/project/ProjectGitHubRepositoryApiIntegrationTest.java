package com.mittechkernel.backend.modules.project;

import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubIdentity;
import com.mittechkernel.backend.common.github.GitHubRepository;
import com.mittechkernel.backend.modules.user.service.GitHubConnectionService;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.github.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class ProjectGitHubRepositoryApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitHubConnectionService gitHubConnectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        jdbcTemplate.update("DELETE FROM projects WHERE name = ?", "Repo Link Test Project");
        clearGitHubConnection("student@kernel.ac.in");
        clearGitHubConnection("core@kernel.ac.in");
    }

    @Test
    void unauthenticatedRepositoryListIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/me/github/repositories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disconnectedUserCannotListRepositories() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/me/github/repositories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void connectedUserCanListRepositories() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);

        String response = mockMvc.perform(get("/api/v1/me/github/repositories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].githubRepoId").exists())
                .andExpect(jsonPath("$.data[0].repoName").value("alpha-repo"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("stub-access-token");
    }

    @Test
    void projectOwnerCanLinkRepository() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);
        Long projectId = createProject(token);

        String response = mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.githubRepoId").value(1234))
                .andExpect(jsonPath("$.data.repoName").value("alpha-repo"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("stub-access-token");
    }

    @Test
    void nonOwnerCannotLinkRepository() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        String outsiderToken = login("core@kernel.ac.in", "Kernel@123");
        connectGithub(ownerToken);
        Long projectId = createProject(ownerToken);

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inaccessibleOrMissingRepositoryIsRejected() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);
        Long projectId = createProject(token);

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":9999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateProjectRepositoryLinkIsRejected() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);
        Long projectId = createProject(token);

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repositoryAlreadyLinkedToAnotherProjectIsRejected() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);
        Long projectOneId = createProject(token);
        Long projectTwoId = createProject(token);

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectOneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectTwoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkedRepositoryCanBeRetrieved() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);
        Long projectId = createProject(token);

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.githubRepoId").value(1234))
                .andExpect(jsonPath("$.data.fullName").value("kernel-org/alpha-repo"));
    }

    @Test
    void ownerCanUnlinkRepository() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);
        Long projectId = createProject(token);

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonOwnerCannotUnlinkRepository() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        String outsiderToken = login("core@kernel.ac.in", "Kernel@123");
        connectGithub(ownerToken);
        Long projectId = createProject(ownerToken);

        mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void githubCredentialsAreNeverExposedInRepositoryResponses() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        connectGithub(token);
        Long projectId = createProject(token);

        String response = mockMvc.perform(post("/api/v1/projects/{id}/github/repository", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubRepoId\":1234}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("stub-access-token");
        assertThat(response).doesNotContain("secret");
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("accessToken").asText();
    }

    private void clearGitHubConnection(String email) throws Exception {
        String token = login(email, "Kernel@123");
        gitHubConnectionService.disconnectUser(getCurrentUserId(token));
    }

    private void connectGithub(String token) throws Exception {
        Long userId = getCurrentUserId(token);
        gitHubConnectionService.connectUser(userId, "stub-access-token",
                new GitHubIdentity(99L, "kernel-user", "Kernel User", null, null, null));
    }

    private Long getCurrentUserId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("id").asLong();
    }

    private Long createProject(String token) throws Exception {
        Long domainId = getDomainId(token);
        String requestBody = "{\"name\":\"Repo Link Test Project\",\"domainId\":" + domainId + ",\"program\":\"TECHNICAL\",\"problem\":\"Need a project\",\"solution\":\"Build it\",\"technologies\":[\"JAVA\"],\"requiredSkills\":[\"SPRING\"],\"expectedMembers\":5,\"outcome\":\"Demo\"}";

        String response = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("id").asLong();
    }

    private Long getDomainId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/domains")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").get(0).path("id").asLong();
    }

    @TestConfiguration
    static class GitHubApiTestConfig {
        @Bean(name = "mockGitHubClient")
        @Primary
        GitHubClient mockGitHubClient() {
            return new GitHubClient() {
                @Override
                public String exchangeCodeForAccessToken(String code, String redirectUri, String codeVerifier) {
                    return "stub-access-token";
                }

                @Override
                public GitHubIdentity getAuthenticatedUser(String accessToken) {
                    return new GitHubIdentity(99L, "kernel-user", "Kernel User", null, null, null);
                }

                @Override
                public List<GitHubRepository> listAccessibleRepositories(String accessToken) {
                    return List.of(
                            new GitHubRepository(1234L, "kernel-org", "alpha-repo", "kernel-org/alpha-repo", "https://github.com/kernel-org/alpha-repo", "PUBLIC"),
                            new GitHubRepository(4321L, "kernel-org", "beta-repo", "kernel-org/beta-repo", "https://github.com/kernel-org/beta-repo", "PRIVATE")
                    );
                }

                @Override
                public GitHubRepository getRepository(String accessToken, Long repositoryId) {
                    return listAccessibleRepositories(accessToken).stream()
                            .filter(repo -> repo.id().equals(repositoryId))
                            .findFirst()
                            .orElse(null);
                }

                @Override
                public List<com.mittechkernel.backend.common.github.GitHubCommit> listCommits(
                        String accessToken, String owner, String repository) {
                    return List.of();
                }
            };
        }
    }
}
