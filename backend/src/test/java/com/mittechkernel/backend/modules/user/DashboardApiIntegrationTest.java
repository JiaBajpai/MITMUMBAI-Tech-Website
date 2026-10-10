package com.mittechkernel.backend.modules.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DashboardApiIntegrationTest {
    private static final String TEST_PASSWORD = "Dashboard@123";

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private Long userId;
    private String email;
    private String projectPrefix;
    private String taskPrefix;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        String suffix = UUID.randomUUID().toString();
        email = "dashboard-test-" + suffix + "@example.test";
        projectPrefix = "dashboard-test-" + suffix;
        taskPrefix = "dashboard-test-" + suffix;
        userId = jdbc.queryForObject("""
                INSERT INTO users (email, name, password_hash, program, active)
                VALUES (?, 'Dashboard Test User', ?, 'TECHNICAL', TRUE) RETURNING id
                """, Long.class, email, passwordEncoder.encode(TEST_PASSWORD));
        jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'STUDENT')", userId);
    }

    @AfterEach
    void cleanUp() {
        if (userId == null) return;
        jdbc.update("DELETE FROM xp_events WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM attendance WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM task_completions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM tasks WHERE title LIKE ?", taskPrefix + "%");
        jdbc.update("DELETE FROM projects WHERE name LIKE ?", projectPrefix + "%");
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
        userId = null;
    }

    @Test
    void dashboardAggregatesCurrentUsersProfileActivityAndMatchesXpAndLeaderboard() throws Exception {
        createProfile();
        createTasksAndCompletions();
        createProjectsAndMemberships();
        createAttendance();
        jdbc.update("INSERT INTO xp_events (user_id, amount, source) VALUES (?, 15, 'TASK'), (?, 23, 'RESOURCE')",
                userId, userId);

        String token = login();
        String xpJson = mockMvc.perform(get("/api/v1/me/xp").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String dashboardJson = mockMvc.perform(get("/api/v1/me/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.userId").value(userId))
                .andExpect(jsonPath("$.data.profile.name").value("Dashboard Test User"))
                .andExpect(jsonPath("$.data.profile.program").value("TECHNICAL"))
                .andExpect(jsonPath("$.data.profile.bio").value("Dashboard biography"))
                .andExpect(jsonPath("$.data.profile.email").doesNotExist())
                .andExpect(jsonPath("$.data.profile.phone").doesNotExist())
                .andExpect(jsonPath("$.data.profile.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.xp.totalXp").value(38))
                .andExpect(jsonPath("$.data.tasks.assignedOpen").value(1))
                .andExpect(jsonPath("$.data.tasks.assignedCompleted").value(1))
                .andExpect(jsonPath("$.data.tasks.assignedVerified").value(1))
                .andExpect(jsonPath("$.data.tasks.submittedCompletions").value(2))
                .andExpect(jsonPath("$.data.tasks.verifiedCompletions").value(1))
                .andExpect(jsonPath("$.data.tasks.unverifiedCompletions").value(1))
                .andExpect(jsonPath("$.data.projects.requestedMemberships").value(1))
                .andExpect(jsonPath("$.data.projects.activeMemberships").value(1))
                .andExpect(jsonPath("$.data.projects.rejectedMemberships").value(1))
                .andExpect(jsonPath("$.data.projects.recentMemberships[?(@.membershipStatus == 'ACTIVE')]").exists())
                .andExpect(jsonPath("$.data.attendance.total").value(4))
                .andExpect(jsonPath("$.data.attendance.present").value(1))
                .andExpect(jsonPath("$.data.attendance.absent").value(1))
                .andExpect(jsonPath("$.data.attendance.late").value(1))
                .andExpect(jsonPath("$.data.attendance.excused").value(1))
                .andExpect(jsonPath("$.data.githubContributions.total").value(2))
                .andExpect(jsonPath("$.data.githubContributions.verified").value(1))
                .andExpect(jsonPath("$.data.githubContributions.unverified").value(1))
                .andExpect(jsonPath("$.data.githubContributions.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.githubContributions.encryptedCredentials").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode xpData = objectMapper.readTree(xpJson).path("data");
        JsonNode dashboardData = objectMapper.readTree(dashboardJson).path("data");
        assertEquals(xpData.path("totalXp").asInt(), dashboardData.path("xp").path("totalXp").asInt());
        assertEquals(xpData.path("breakdown"), dashboardData.path("xp").path("breakdown"));
        assertNotNull(dashboardData.path("leaderboardRank").numberValue());

        String leaderboardJson = mockMvc.perform(get("/api/v1/leaderboard")
                        .header("Authorization", "Bearer " + token).param("size", "100"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(rankOf(objectMapper.readTree(leaderboardJson).path("data").path("entries"), userId),
                dashboardData.path("leaderboardRank").asLong());
    }

    @Test
    void newUserGetsCleanEmptyStateAndEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/me/dashboard")).andExpect(status().isUnauthorized());

        String token = login();
        mockMvc.perform(get("/api/v1/me/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.xp.totalXp").value(0))
                .andExpect(jsonPath("$.data.tasks.assignedOpen").value(0))
                .andExpect(jsonPath("$.data.tasks.submittedCompletions").value(0))
                .andExpect(jsonPath("$.data.projects.recentMemberships").isEmpty())
                .andExpect(jsonPath("$.data.attendance.total").value(0))
                .andExpect(jsonPath("$.data.githubContributions.total").value(0))
                .andExpect(jsonPath("$.data.leaderboardRank").isNumber());
    }

    private void createProfile() {
        jdbc.update("""
                INSERT INTO user_profiles (user_id, bio, avatar_url, github_url, linkedin_url, phone)
                VALUES (?, 'Dashboard biography', 'https://example.test/avatar.png',
                        'https://github.com/dashboard-user', 'https://linkedin.com/in/dashboard-user', 'private-phone')
                """, userId);
    }

    private void createTasksAndCompletions() {
        long sessionId = sessionId("PostgreSQL Fundamentals");
        long[] taskIds = new long[3];
        String[] states = {"OPEN", "COMPLETED", "VERIFIED"};
        for (int i = 0; i < states.length; i++) {
            taskIds[i] = jdbc.queryForObject("""
                    INSERT INTO tasks (session_id, title, status, assignee_id)
                    VALUES (?, ?, ?, ?) RETURNING id
                    """, Long.class, sessionId, taskPrefix + "-task-" + i, states[i], userId);
        }
        jdbc.update("""
                INSERT INTO task_completions (task_id, user_id, repo_url, verified, verified_by, verified_at)
                VALUES (?, ?, 'https://example.test/repo', TRUE, ?, now()),
                       (?, ?, 'https://example.test/repo', FALSE, NULL, NULL)
                """, taskIds[1], userId, seedUserId("lead.backend@kernel.ac.in"), taskIds[2], userId);
    }

    private void createProjectsAndMemberships() {
        String[] states = {"ACTIVE", "REQUESTED", "REJECTED"};
        for (int i = 0; i < states.length; i++) {
            long projectId = jdbc.queryForObject("""
                    INSERT INTO projects (name, domain_id, status, program, problem, solution, created_by)
                    VALUES (?, ?, 'ACTIVE', 'TECHNICAL', 'dashboard test', 'dashboard summary', ?) RETURNING id
                    """, Long.class, projectPrefix + "-project-" + i, domainId("Backend"),
                    seedUserId("superadmin@kernel.ac.in"));
            jdbc.update("INSERT INTO project_members (project_id, user_id, role, status) VALUES (?, ?, 'MEMBER', ?)",
                    projectId, userId, states[i]);
            if (i == 0) createLinkedRepository(projectId);
        }
    }

    private void createLinkedRepository(long projectId) {
        long linkedBy = seedUserId("superadmin@kernel.ac.in");
        long repositoryId = jdbc.queryForObject("""
                INSERT INTO project_github_repositories (project_id, github_repo_id, owner_login, repo_name,
                    full_name, html_url, visibility, linked_by)
                VALUES (?, ?, 'kernel-test', 'dashboard-test', 'kernel-test/dashboard-test',
                    'https://github.com/kernel-test/dashboard-test', 'PUBLIC', ?) RETURNING id
                """, Long.class, projectId, Math.abs(UUID.randomUUID().getMostSignificantBits()), linkedBy);
        long githubId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        jdbc.update("INSERT INTO github_identities (user_id, github_user_id, github_login) VALUES (?, ?, 'dashboard-test-user')",
                userId, githubId);
        insertContribution(projectId, repositoryId, githubId, "a".repeat(40), true, linkedBy);
        insertContribution(projectId, repositoryId, githubId, "b".repeat(40), false, null);
    }

    private void insertContribution(long projectId, long repositoryId, long githubId, String sha,
                                    boolean verified, Long verifierId) {
        jdbc.update("""
                INSERT INTO github_contributions (project_id, project_github_repository_id, user_id,
                    github_author_id, github_author_login, commit_sha, commit_url, commit_message,
                    committed_at, verified, verified_by, verified_at)
                VALUES (?, ?, ?, ?, 'dashboard-test-user', ?, ?, 'dashboard test commit', now(), ?, ?,
                    CASE WHEN ? THEN now() ELSE NULL END)
                """, projectId, repositoryId, userId, githubId, sha,
                "https://github.com/kernel-test/dashboard-test/commit/" + sha,
                verified, verifierId, verified);
    }

    private void createAttendance() {
        String[] topics = {"REST APIs with Spring Boot", "PostgreSQL Fundamentals",
                "Intro to Machine Learning", "Algorithmic Problem Solving"};
        String[] states = {"PRESENT", "ABSENT", "LATE", "EXCUSED"};
        for (int i = 0; i < topics.length; i++) {
            jdbc.update("INSERT INTO attendance (session_id, user_id, status) VALUES (?, ?, ?)",
                    sessionId(topics[i]), userId, states[i]);
        }
    }

    private long sessionId(String topic) {
        return jdbc.queryForObject("SELECT id FROM sessions WHERE topic = ? ORDER BY id LIMIT 1", Long.class, topic);
    }

    private long domainId(String name) {
        return jdbc.queryForObject("SELECT id FROM domains WHERE name = ?", Long.class, name);
    }

    private long seedUserId(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private String login() throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + TEST_PASSWORD + "\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private long rankOf(JsonNode entries, long id) {
        for (JsonNode entry : entries) {
            if (entry.path("userId").asLong() == id) return entry.path("rank").asLong();
        }
        throw new AssertionError("Current user is missing from the leaderboard");
    }
}
