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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class FoundationProgramIntegrationTest {
    private static final String PASSWORD = "Foundation@123";

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Long participantId;
    private Long sessionId;
    private Long projectId;
    private String email;
    private String prefix;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        String suffix = UUID.randomUUID().toString();
        prefix = "foundation-test-" + suffix;
        email = prefix + "@example.test";
        participantId = jdbc.queryForObject("""
                INSERT INTO users (email, name, password_hash, program, active)
                VALUES (?, 'Foundation Test Participant', ?, 'FOUNDATION', TRUE) RETURNING id
                """, Long.class, email, passwordEncoder.encode(PASSWORD));
        jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'STUDENT')", participantId);
        Long backendDomainId = jdbc.queryForObject("SELECT id FROM domains WHERE name = 'Backend'", Long.class);
        sessionId = jdbc.queryForObject("""
                INSERT INTO sessions (domain_id, program, topic, type, date, time)
                VALUES (?, 'FOUNDATION', ?, 'FOUNDATION', CURRENT_DATE, TIME '10:00') RETURNING id
                """, Long.class, backendDomainId, prefix + " Foundation Basics");
    }

    @AfterEach
    void cleanUp() {
        if (participantId == null) return;
        if (projectId != null) jdbc.update("DELETE FROM projects WHERE id = ?", projectId);
        jdbc.update("DELETE FROM xp_events WHERE user_id = ?", participantId);
        jdbc.update("DELETE FROM attendance WHERE user_id = ?", participantId);
        jdbc.update("DELETE FROM tasks WHERE title LIKE ?", prefix + "%");
        jdbc.update("DELETE FROM sessions WHERE id = ?", sessionId);
        jdbc.update("DELETE FROM users WHERE id = ?", participantId);
        participantId = null;
    }

    @Test
    void foundationParticipantCanReadCompleteAndEarnOnlyFoundationActivityXp() throws Exception {
        String coreToken = login("core@kernel.ac.in", "Kernel@123");
        String participantToken = login(email, PASSWORD);
        Long taskId = createFoundationTask(coreToken, participantId);

        mockMvc.perform(get("/api/v1/sessions").param("program", "FOUNDATION")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + sessionId + ")].program").value("FOUNDATION"));
        mockMvc.perform(get("/api/v1/tasks").param("sessionId", sessionId.toString())
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + taskId + ")].status").value("OPEN"));

        String completion = "{\"repoUrl\":\"https://example.test/foundation-work\"}";
        mockMvc.perform(post("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON).content(completion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(post("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON).content(completion))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/tasks/{id}/verify", taskId)
                        .header("Authorization", "Bearer " + coreToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
        mockMvc.perform(patch("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest());

        String attendance = "{\"userId\":" + participantId + ",\"status\":\"PRESENT\"}";
        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(attendance))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(attendance))
                .andExpect(status().isBadRequest());

        String foundationBoard = mockMvc.perform(get("/api/v1/leaderboard")
                        .param("program", "FOUNDATION").param("size", "100")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + participantId + ")].totalXp").value(20))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/v1/leaderboard").param("program", "TECHNICAL").param("size", "100")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[?(@.userId == " + participantId + ")].userId").doesNotExist());
        JsonNode boardData = objectMapper.readTree(foundationBoard).path("data");
        org.junit.jupiter.api.Assertions.assertEquals("FOUNDATION", boardData.path("entries").get(0).path("program").asText());
    }

    @Test
    void foundationTaskAndAttendanceAuthorizationUsesProgramAndFacultyIsReadOnly() throws Exception {
        String coreToken = login("core@kernel.ac.in", "Kernel@123");
        String facultyToken = login("faculty@kernel.ac.in", "Kernel@123");
        String leadToken = login("lead.backend@kernel.ac.in", "Kernel@123");
        String participantToken = login(email, PASSWORD);
        Long taskId = createFoundationTask(coreToken, participantId);

        mockMvc.perform(get("/api/v1/sessions/{id}", sessionId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.program").value("FOUNDATION"));
        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Faculty edit\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repoUrl\":\"https://example.test/faculty-work\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + participantId + ",\"status\":\"PRESENT\"}"))
                .andExpect(status().isForbidden());

        String foundationTask = "{\"sessionId\":" + sessionId + ",\"title\":\"" + prefix
                + "-lead-task\",\"requirements\":[\"Practice\"],\"assigneeId\":" + participantId + "}";
        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + leadToken)
                        .contentType(MediaType.APPLICATION_JSON).content(foundationTask))
                .andExpect(status().isForbidden());

        Long technicalUser = jdbc.queryForObject("SELECT id FROM users WHERE email = 'student@kernel.ac.in'", Long.class);
        String mismatchedTask = "{\"sessionId\":" + sessionId + ",\"title\":\"" + prefix
                + "-mismatch-task\",\"requirements\":[\"Practice\"],\"assigneeId\":" + technicalUser + "}";
        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mismatchedTask))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + technicalUser + ",\"status\":\"PRESENT\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/sessions").param("program", "FOUNDATION")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk());
    }

    @Test
    void taskAssignmentAndProjectMembershipApprovalRequireProgramAlignment() throws Exception {
        String coreToken = login("core@kernel.ac.in", "Kernel@123");
        Long technicalSessionId = jdbc.queryForObject("SELECT id FROM sessions WHERE program = 'TECHNICAL' ORDER BY id LIMIT 1", Long.class);
        String crossProgramTask = "{\"sessionId\":" + technicalSessionId + ",\"title\":\"" + prefix
                + "-technical-task\",\"requirements\":[\"Practice\"],\"assigneeId\":" + participantId + "}";
        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(crossProgramTask))
                .andExpect(status().isBadRequest());

        Long domainId = jdbc.queryForObject("SELECT domain_id FROM sessions WHERE id = ?", Long.class, sessionId);
        String projectBody = "{\"name\":\"" + prefix + "-technical-project\",\"domainId\":" + domainId
                + ",\"program\":\"TECHNICAL\",\"problem\":\"Need a project\",\"solution\":\"Build it\",\"expectedMembers\":2}";
        String projectResponse = mockMvc.perform(post("/api/v1/projects").header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(projectBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        projectId = objectMapper.readTree(projectResponse).path("data").path("id").asLong();

        String crossProgramFoundationTask = "{\"sessionId\":" + sessionId + ",\"projectId\":" + projectId
                + ",\"title\":\"" + prefix + "-cross-program-task\",\"requirements\":[\"Practice\"]}";
        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(crossProgramFoundationTask))
                .andExpect(status().isBadRequest());

        jdbc.update("INSERT INTO project_members (project_id, user_id, role, status) VALUES (?, ?, 'MEMBER', 'REQUESTED')",
                projectId, participantId);
        mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}/approve", projectId, participantId)
                        .header("Authorization", "Bearer " + coreToken))
                .andExpect(status().isForbidden());
    }

    private Long createFoundationTask(String coreToken, Long assigneeId) throws Exception {
        String body = "{\"sessionId\":" + sessionId + ",\"title\":\"" + prefix
                + "-task\",\"requirements\":[\"Practice safely\"],\"assigneeId\":" + assigneeId + "}";
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + coreToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private String login(String account, String password) throws Exception {
        String body = "{\"email\":\"" + account + "\",\"password\":\"" + password + "\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }
}
