package com.mittechkernel.backend.modules.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TaskApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.mittechkernel.backend.modules.gamification.XpRepository xpRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanListTasks() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").exists());
    }

    @Test
    void authenticatedUserCanReadTask() throws Exception {
        Long taskId = findFirstTaskId();
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId));
    }

    @Test
    void leadCanCreateTaskInAssignedDomain() throws Exception {
        Long sessionId = findSessionIdForDomain("Backend");
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        String body = "{\"sessionId\":\"" + sessionId + "\",\"title\":\"New API task\",\"requirements\":[\"Build endpoint\"],\"deadline\":\"2026-12-31T18:00:00Z\",\"verificationRequired\":true,\"assigneeId\":" + findUserId("student@kernel.ac.in") + "}";

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("New API task"));
    }

    @Test
    void studentCannotModifyAnotherUsersTask() throws Exception {
        Long taskId = createTaskForLead("lead.backend@kernel.ac.in", "Backend", "Student blocked task");
        String token = login("student@kernel.ac.in", "Kernel@123");

        String body = "{\"status\":\"VERIFIED\"}";

        mockMvc.perform(patch("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void taskAssigneeCannotReassignOrEditTheTask() throws Exception {
        Long taskId = createTaskAssignedToStudent("Assignee task mutation");
        String studentToken = login("student@kernel.ac.in", "Kernel@123");
        Long facultyId = findUserId("faculty@kernel.ac.in");

        mockMvc.perform(patch("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + facultyId + "}"))
                .andExpect(status().isForbidden());
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM tasks WHERE id = ?", Long.class, taskId))
                .isEqualTo(findUserId("student@kernel.ac.in"));
    }

    @Test
    void invalidTaskIdReturns404() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/tasks/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidStateTransitionIsRejected() throws Exception {
        Long taskId = createTaskForLead("lead.backend@kernel.ac.in", "Backend", "Workflow invalid transition");
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        String body = "{\"status\":\"OPEN\"}";

        mockMvc.perform(patch("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void taskCannotBeMarkedVerifiedThroughGenericPatch() throws Exception {
        Long taskId = createTaskForLead("lead.backend@kernel.ac.in", "Backend", "Patch verification bypass");
        String leadToken = login("lead.backend@kernel.ac.in", "Kernel@123");

        mockMvc.perform(patch("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + leadToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, taskId))
                .isEqualTo("OPEN");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM task_completions WHERE task_id = ?", Integer.class, taskId))
                .isZero();
    }

    @Test
    void verificationRequiredTaskUsesProjectRepositoryAndCompleterContribution() throws Exception {
        String leadToken = login("lead.backend@kernel.ac.in", "Kernel@123");
        String studentToken = login("student@kernel.ac.in", "Kernel@123");
        Long sessionId = findSessionIdForDomain("Backend");
        Long leadId = findUserId("lead.backend@kernel.ac.in");
        Long studentId = findUserId("student@kernel.ac.in");
        int initialStudentXp = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount), 0)::integer FROM xp_events WHERE user_id = ?", Integer.class, studentId);
        Long domainId = jdbcTemplate.queryForObject("SELECT domain_id FROM sessions WHERE id = ?", Long.class, sessionId);
        String projectBody = "{\"name\":\"Task verify " + System.nanoTime()
                + "\",\"domainId\":" + domainId
                + ",\"program\":\"TECHNICAL\",\"problem\":\"test\",\"solution\":\"test\","
                + "\"technologies\":[\"Java\"],\"requiredSkills\":[\"Spring\"],\"expectedMembers\":2}";
        String projectResponse = mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + leadToken)
                        .contentType(MediaType.APPLICATION_JSON).content(projectBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long projectId = objectMapper.readTree(projectResponse).path("data").path("id").asLong();
        Long repositoryId = jdbcTemplate.queryForObject("""
                INSERT INTO project_github_repositories
                    (project_id, github_repo_id, owner_login, repo_name, full_name, html_url, visibility, linked_by)
                VALUES (?, ?, 'kernel-org', 'task-repo', 'kernel-org/task-repo', 'https://github.com/kernel-org/task-repo', 'PUBLIC', ?)
                RETURNING id
                """, Long.class, projectId, System.nanoTime(), leadId);
        jdbcTemplate.update("INSERT INTO project_members (project_id, user_id, role, status, created_at, updated_at) VALUES (?, ?, 'MEMBER', 'ACTIVE', now(), now()) ON CONFLICT (project_id, user_id) DO UPDATE SET status = 'ACTIVE'",
                projectId, studentId);
        jdbcTemplate.update("INSERT INTO github_identities (user_id, github_user_id, github_login) VALUES (?, 99001, 'task-author') ON CONFLICT (user_id) DO UPDATE SET github_user_id = 99001, github_login = 'task-author'",
                studentId);
        String sha = "c".repeat(40);
        jdbcTemplate.update("""
                INSERT INTO github_contributions
                    (project_id, project_github_repository_id, user_id, github_author_id, github_author_login,
                     commit_sha, commit_url, commit_message, committed_at)
                VALUES (?, ?, ?, 99001, 'task-author', ?, 'https://github.com/kernel-org/task-repo/commit/' || ?, 'task commit', now())
                """, projectId, repositoryId, studentId, sha, sha);

        String taskBody = "{\"sessionId\":" + sessionId + ",\"projectId\":" + projectId
                + ",\"title\":\"Verified GitHub task\",\"requirements\":[\"Commit work\"],"
                + "\"verificationRequired\":true,\"assigneeId\":" + studentId + "}";
        String taskResponse = mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + leadToken)
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskResponse).path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/tasks/{id}/complete", taskId).header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repoUrl\":\"https://untrusted.example/ignored\",\"commitSha\":\"" + sha + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tasks/{id}/verify", taskId).header("Authorization", "Bearer " + leadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
        assertThat(jdbcTemplate.queryForObject("SELECT verified FROM task_completions WHERE task_id = ?", Boolean.class, taskId)).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT verified FROM github_contributions WHERE project_id = ? AND commit_sha = ?", Boolean.class, projectId, sha)).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount), 0)::integer FROM xp_events WHERE user_id = ?", Integer.class, studentId) - initialStudentXp).isEqualTo(55);
        Long completionId = jdbcTemplate.queryForObject("SELECT id FROM task_completions WHERE task_id = ?", Long.class, taskId);
        assertThat(xpRepository.awardVerifiedTaskCompletion(completionId)).isFalse();
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO xp_events (user_id, amount, source, ref_type, ref_id) VALUES (?, 15, 'TASK', 'TASK_COMPLETION', ?)", leadId, completionId))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("INSERT INTO xp_events (user_id, amount, source, ref_type, ref_id) VALUES (?, 5, 'SESSION', 'TASK_COMPLETION', ?)", studentId, completionId);
        Integer expectedTotalXp = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0)::integer FROM xp_events WHERE user_id = ?", Integer.class, studentId);

        mockMvc.perform(get("/api/v1/me/xp").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.breakdown.TASK").exists())
                .andExpect(jsonPath("$.data.totalXp").value(expectedTotalXp));
        mockMvc.perform(post("/api/v1/me/xp").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":999999,\"userId\":" + studentId + "}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void xpAwardRollsBackWithItsActivityTransaction() {
        Long sessionId = findSessionIdForDomain("Backend");
        Long studentId = findUserId("student@kernel.ac.in");
        Long verifierId = findUserId("lead.backend@kernel.ac.in");
        Long taskId = jdbcTemplate.queryForObject("""
                INSERT INTO tasks (session_id, title, requirements, verification_required, assignee_id, status)
                VALUES (?, 'XP rollback fixture', '{}', false, ?, 'COMPLETED') RETURNING id
                """, Long.class, sessionId, studentId);
        Long completionId = jdbcTemplate.queryForObject("""
                INSERT INTO task_completions (task_id, user_id, repo_url)
                VALUES (?, ?, 'https://example.test/repo') RETURNING id
                """, Long.class, taskId, studentId);

        assertThat(xpRepository.awardVerifiedTaskCompletion(completionId)).isFalse();
        jdbcTemplate.update("UPDATE task_completions SET verified = true, verified_by = ?, verified_at = now() WHERE id = ?", verifierId, completionId);
        jdbcTemplate.update("UPDATE tasks SET status = 'VERIFIED' WHERE id = ?", taskId);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            assertThat(xpRepository.awardVerifiedTaskCompletion(completionId)).isTrue();
            throw new IllegalStateException("force transaction rollback");
        }));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM xp_events WHERE source = 'TASK' AND ref_type = 'TASK_COMPLETION' AND ref_id = ?",
                Integer.class, completionId)).isZero();
        jdbcTemplate.update("DELETE FROM task_completions WHERE id = ?", completionId);
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", taskId);
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

    private Long findFirstTaskId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tasks ORDER BY created_at DESC LIMIT 1",
                Long.class
        );
    }

    private Long findSessionIdForDomain(String domainName) {
        return jdbcTemplate.queryForObject(
                "SELECT s.id FROM sessions s JOIN domains d ON d.id = s.domain_id WHERE d.name = ? ORDER BY s.created_at DESC LIMIT 1",
                Long.class,
                domainName
        );
    }

    private Long findUserId(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }

    private Long findTaskForUser(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT t.id FROM tasks t JOIN users u ON u.id = t.assignee_id WHERE u.email = ? ORDER BY t.id LIMIT 1",
                Long.class,
                email
        );
    }

    private Long createTaskForLead(String email, String domainName, String title) throws Exception {
        Long sessionId = findSessionIdForDomain(domainName);
        Long assigneeId = findUserId(email);
        String token = login(email, "Kernel@123");

        String body = "{\"sessionId\":" + sessionId + ",\"title\":\"" + title + "\",\"requirements\":[\"Do the work\"],\"deadline\":\"2026-12-31T18:00:00Z\",\"verificationRequired\":true,\"assigneeId\":" + assigneeId + "}";

        String response = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("id").asLong();
    }

    private Long createTaskAssignedToStudent(String title) throws Exception {
        Long sessionId = findSessionIdForDomain("Backend");
        String leadToken = login("lead.backend@kernel.ac.in", "Kernel@123");
        String body = "{\"sessionId\":" + sessionId + ",\"title\":\"" + title
                + "\",\"requirements\":[\"Do the work\"],\"assigneeId\":"
                + findUserId("student@kernel.ac.in") + "}";
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + leadToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }
}
