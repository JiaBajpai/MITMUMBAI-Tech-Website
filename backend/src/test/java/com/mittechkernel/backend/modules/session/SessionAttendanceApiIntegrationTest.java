package com.mittechkernel.backend.modules.session;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SessionAttendanceApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void unauthenticatedSessionRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanListSessions() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].topic").exists());
    }

    @Test
    void sessionNotFoundReturns404() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/sessions/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidSessionFilterIsRejected() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + token)
                        .param("program", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedUserCanAccessTheirAttendanceState() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/me/attendance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(findUserId("student@kernel.ac.in")));
    }

    @Test
    void studentCannotMarkAnotherUsersAttendance() throws Exception {
        Long sessionId = findSessionIdForTopic("REST APIs with Spring Boot");
        String token = login("student@kernel.ac.in", "Kernel@123");

        String body = "{\"userId\":" + findUserId("lead.backend@kernel.ac.in") + ",\"status\":\"PRESENT\"}";

        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void domainLeadCanManageAttendanceWithinAssignedDomain() throws Exception {
        Long sessionId = findSessionIdForTopic("PostgreSQL Fundamentals");
        Long userId = findUserId("faculty@kernel.ac.in");
        jdbcTemplate.update("DELETE FROM attendance WHERE session_id = ? AND user_id = ?", sessionId, userId);
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        String body = "{\"userId\":" + userId + ",\"status\":\"LATE\"}";

        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("LATE"));
    }

    @Test
    void domainLeadCannotManageSessionOutsideAssignedDomain() throws Exception {
        Long sessionId = findSessionIdForTopic("React State Management");
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        String body = "{\"userId\":" + findUserId("student@kernel.ac.in") + ",\"status\":\"PRESENT\"}";

        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateAttendanceIsRejected() throws Exception {
        Long sessionId = findSessionIdForTopic("REST APIs with Spring Boot");
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        String body = "{\"userId\":" + findUserId("student@kernel.ac.in") + ",\"status\":\"PRESENT\"}";

        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAttendanceStatusIsRejected() throws Exception {
        Long sessionId = findSessionIdForTopic("REST APIs with Spring Boot");
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        String body = "{\"userId\":" + findUserId("faculty@kernel.ac.in") + ",\"status\":\"AWOL\"}";

        mockMvc.perform(post("/api/v1/sessions/{id}/attendance", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
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

    private Long findSessionIdForTopic(String topic) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sessions WHERE topic = ? ORDER BY date DESC LIMIT 1",
                Long.class,
                topic
        );
    }

    private Long findUserId(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }
}
