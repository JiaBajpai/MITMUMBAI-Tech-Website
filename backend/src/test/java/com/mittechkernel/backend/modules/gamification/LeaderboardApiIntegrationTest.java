package com.mittechkernel.backend.modules.gamification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LeaderboardApiIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mockMvc;
    private Long foundationSessionId;
    private Long backendDomainId;
    private Long frontendDomainId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        backendDomainId = domainId("Backend");
        frontendDomainId = domainId("Frontend");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM xp_events WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)", "leaderboard-test-%");
        jdbc.update("DELETE FROM domain_members WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)", "leaderboard-test-%");
        jdbc.update("DELETE FROM attendance WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)", "leaderboard-test-%");
        jdbc.update("DELETE FROM tasks WHERE title LIKE ?", "leaderboard-test-%");
        jdbc.update("DELETE FROM projects WHERE name LIKE ?", "leaderboard-test-project-%");
        if (foundationSessionId != null) jdbc.update("DELETE FROM sessions WHERE id = ?", foundationSessionId);
        jdbc.update("DELETE FROM users WHERE email LIKE ?", "leaderboard-test-%");
        foundationSessionId = null;
    }

    @Test
    void overallAggregatesRanksPaginatesAndExcludesInactiveAccounts() throws Exception {
        long alpha = user("Alpha Leader", "TECHNICAL", true);
        long beta = user("Beta Leader", "TECHNICAL", true);
        long inactive = user("Inactive Leader", "TECHNICAL", false);
        user("Zero XP Leader", "TECHNICAL", true);
        user("Foundation Leader", "FOUNDATION", true);
        createVerifiedTaskXp(alpha, "TECHNICAL", 100, "alpha-a");
        createVerifiedTaskXp(alpha, "TECHNICAL", 40, "alpha-b");
        createVerifiedTaskXp(beta, "TECHNICAL", 140, "beta");
        xp(inactive, 10000, "TASK");

        String token = login("student@kernel.ac.in");
        String leaderboardJson = mockMvc.perform(get("/api/v1/leaderboard").header("Authorization", "Bearer " + token)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + alpha + ")].totalXp").value(140))
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + inactive + ")].userId").doesNotExist())
                .andExpect(jsonPath("$.data.entries[?(@.name == 'Zero XP Leader')].totalXp").value(0))
                .andExpect(jsonPath("$.data.entries[?(@.name == 'Foundation Leader')].userId").doesNotExist())
                .andExpect(jsonPath("$.data.entries[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.entries[0].passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode entries = objectMapper.readTree(leaderboardJson).path("data").path("entries");
        long alphaRank = rankOf(entries, alpha);
        long betaRank = rankOf(entries, beta);
        assertTrue(alphaRank < betaRank, "equal XP is ordered by public profile name");
        assertEquals(alphaRank + 1, betaRank, "tied users are adjacent in the stable order");

        mockMvc.perform(get("/api/v1/leaderboard").header("Authorization", "Bearer " + token)
                        .param("page", Long.toString(alphaRank - 1)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[0].userId").value(alpha));
        mockMvc.perform(get("/api/v1/leaderboard").header("Authorization", "Bearer " + token)
                        .param("page", Long.toString(betaRank - 1)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[0].userId").value(beta));
        mockMvc.perform(get("/api/v1/leaderboard").header("Authorization", "Bearer " + token)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/leaderboard").header("Authorization", "Bearer " + token)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/leaderboard").param("program", "FOUNDATION")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[0].name").value("Foundation Leader"));
    }

    @Test
    void programLeaderboardsCountOnlyXpFromMatchingVerifiedActivities() throws Exception {
        long foundationUser = user("Foundation Attributed", "FOUNDATION", true);
        long technicalUser = user("Technical Attributed", "TECHNICAL", true);
        foundationSessionId = createFoundationSession();
        long technicalSession = sessionId("PostgreSQL Fundamentals");

        long foundationAttendance = attendance(foundationUser, foundationSessionId);
        xp(foundationUser, 5, "SESSION", "ATTENDANCE", foundationAttendance);
        long crossProgramTechnicalAttendance = attendance(foundationUser, technicalSession);
        xp(foundationUser, 500, "SESSION", "ATTENDANCE", crossProgramTechnicalAttendance);
        long crossProgramFoundationAttendance = attendance(technicalUser, foundationSessionId);
        xp(technicalUser, 900, "SESSION", "ATTENDANCE", crossProgramFoundationAttendance);
        long technicalAttendance = attendance(technicalUser, sessionId("React State Management"));
        xp(technicalUser, 5, "SESSION", "ATTENDANCE", technicalAttendance);

        createVerifiedTaskXp(foundationUser, "FOUNDATION", 15, "foundation-task");
        createVerifiedTaskXp(technicalUser, "TECHNICAL", 10, "technical-task");
        createVerifiedContributionXp(foundationUser, "FOUNDATION", 40, "foundation-contribution");
        createVerifiedContributionXp(technicalUser, "TECHNICAL", 40, "technical-contribution");
        xp(foundationUser, 20000, "TASK");
        xp(technicalUser, 30000, "RESOURCE");

        String token = login("student@kernel.ac.in");
        mockMvc.perform(get("/api/v1/leaderboard").param("program", "FOUNDATION")
                        .param("size", "100").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + foundationUser + ")].totalXp").value(60))
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + technicalUser + ")].userId").doesNotExist());

        mockMvc.perform(get("/api/v1/leaderboard").param("program", "TECHNICAL")
                        .param("size", "100").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + technicalUser + ")].totalXp").value(55))
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + foundationUser + ")].userId").doesNotExist());
    }

    @Test
    void domainLeaderboardUsesAttributedTechnicalXpAndHonorsLeadScope() throws Exception {
        long technical = user("Domain Activity", "TECHNICAL", true);
        long zero = user("Domain Zero", "TECHNICAL", true);
        long foundation = user("Foundation Activity", "FOUNDATION", true);
        jdbc.update("INSERT INTO domain_members (user_id, domain_id) VALUES (?, ?)", zero, backendDomainId);

        long backendSession = sessionId("PostgreSQL Fundamentals");
        foundationSessionId = jdbc.queryForObject("INSERT INTO sessions (domain_id, program, topic, type, date, time) VALUES (?, 'FOUNDATION', ?, 'FOUNDATION', CURRENT_DATE, TIME '12:00') RETURNING id",
                Long.class, backendDomainId, "leaderboard-test-foundation-" + UUID.randomUUID());
        long technicalAttendance = attendance(technical, backendSession);
        long foundationAttendance = attendance(foundation, foundationSessionId);
        xp(technical, 25, "SESSION", "ATTENDANCE", technicalAttendance);
        xp(foundation, 9000, "SESSION", "ATTENDANCE", foundationAttendance);

        String studentToken = login("student@kernel.ac.in");
        mockMvc.perform(get("/api/v1/leaderboard/domains/{id}", backendDomainId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + technical + ")].totalXp").value(25))
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + zero + ")].totalXp").value(0))
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + foundation + ")].userId").doesNotExist());

        String leadToken = login("lead.backend@kernel.ac.in");
        mockMvc.perform(get("/api/v1/leaderboard/domains/{id}", frontendDomainId)
                        .header("Authorization", "Bearer " + leadToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void leaderboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard")).andExpect(status().isUnauthorized());
    }

    private long user(String name, String program, boolean active) {
        String email = "leaderboard-test-" + UUID.randomUUID() + "@example.test";
        Long id = jdbc.queryForObject("INSERT INTO users (email, name, password_hash, program, active) VALUES (?, ?, 'unused', ?, ?) RETURNING id",
                Long.class, email, name, program, active);
        return id;
    }

    private void xp(long userId, int amount, String source) {
        xp(userId, amount, source, null, null);
    }

    private void xp(long userId, int amount, String source, String refType, Long refId) {
        jdbc.update("INSERT INTO xp_events (user_id, amount, source, ref_type, ref_id) VALUES (?, ?, ?, ?, ?)",
                userId, amount, source, refType, refId);
    }

    private long attendance(long userId, long sessionId) {
        Long id = jdbc.queryForObject("INSERT INTO attendance (session_id, user_id, status) VALUES (?, ?, 'PRESENT') RETURNING id",
                Long.class, sessionId, userId);
        return id;
    }

    private long createFoundationSession() {
        return jdbc.queryForObject("""
                INSERT INTO sessions (domain_id, program, topic, type, date, time)
                VALUES (?, 'FOUNDATION', ?, 'FOUNDATION', CURRENT_DATE, TIME '12:00') RETURNING id
                """, Long.class, backendDomainId, "leaderboard-test-foundation-" + UUID.randomUUID());
    }

    private void createVerifiedTaskXp(long userId, String program, int amount, String key) {
        long session = "FOUNDATION".equals(program) ? foundationSessionId : sessionId("PostgreSQL Fundamentals");
        long verifier = jdbc.queryForObject("SELECT id FROM users WHERE email = 'lead.backend@kernel.ac.in'", Long.class);
        long taskId = jdbc.queryForObject("""
                INSERT INTO tasks (session_id, title, status, assignee_id)
                VALUES (?, ?, 'VERIFIED', ?) RETURNING id
                """, Long.class, session, "leaderboard-test-" + key, userId);
        long completionId = jdbc.queryForObject("""
                INSERT INTO task_completions (task_id, user_id, repo_url, verified, verified_by, verified_at)
                VALUES (?, ?, 'https://example.test/repo', TRUE, ?, now()) RETURNING id
                """, Long.class, taskId, userId, verifier);
        xp(userId, amount, "TASK", "TASK_COMPLETION", completionId);
    }

    private void createVerifiedContributionXp(long userId, String program, int amount, String key) {
        long verifier = jdbc.queryForObject("SELECT id FROM users WHERE email = 'superadmin@kernel.ac.in'", Long.class);
        long projectId = jdbc.queryForObject("""
                INSERT INTO projects (name, domain_id, status, program, problem, solution, created_by)
                VALUES (?, ?, 'ACTIVE', ?, 'test problem', 'test solution', ?) RETURNING id
                """, Long.class, "leaderboard-test-project-" + key, backendDomainId, program, verifier);
        long repoId = jdbc.queryForObject("""
                INSERT INTO project_github_repositories (project_id, github_repo_id, owner_login, repo_name,
                    full_name, html_url, visibility, linked_by)
                VALUES (?, ?, 'test-owner', ?, ?, 'https://github.com/test-owner/test-repo', 'PUBLIC', ?)
                RETURNING id
                """, Long.class, projectId, Math.abs(UUID.randomUUID().getMostSignificantBits()), key,
                "test-owner/" + key, verifier);
        long githubId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        jdbc.update("INSERT INTO github_identities (user_id, github_user_id, github_login) VALUES (?, ?, ?)",
                userId, githubId, "leaderboard-" + key);
        String sha = "e".repeat(40);
        long contributionId = jdbc.queryForObject("""
                INSERT INTO github_contributions (project_id, project_github_repository_id, user_id,
                    github_author_id, github_author_login, commit_sha, commit_url, commit_message,
                    committed_at, verified, verified_by, verified_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'verified leaderboard test', now(), TRUE, ?, now()) RETURNING id
                """, Long.class, projectId, repoId, userId, githubId, "leaderboard-" + key, sha,
                "https://github.com/test-owner/" + key + "/commit/" + sha, verifier);
        xp(userId, amount, "CONTRIBUTION", "GITHUB_CONTRIBUTION", contributionId);
    }

    private long domainId(String name) {
        return jdbc.queryForObject("SELECT id FROM domains WHERE name = ?", Long.class, name);
    }

    private long sessionId(String topic) {
        return jdbc.queryForObject("SELECT id FROM sessions WHERE topic = ? ORDER BY id LIMIT 1", Long.class, topic);
    }

    private String login(String email) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"Kernel@123\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login").header("Origin", "http://localhost:5173").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("accessToken").asText();
    }

    private long rankOf(JsonNode entries, long userId) {
        for (JsonNode entry : entries) {
            if (entry.path("userId").asLong() == userId) return entry.path("rank").asLong();
        }
        throw new AssertionError("Leaderboard entry missing for user " + userId);
    }
}
