package com.mittechkernel.backend.modules.auth;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RbacIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/test/student"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanAccessStudentEndpoint() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/test/student")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void studentAccessingPrivilegedEndpointIsForbidden() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/test/core-member")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void coreMemberCanAccessCoreMemberEndpoint() throws Exception {
        String token = login("core@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/test/core-member")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void facultyCanAccessFacultyEndpoint() throws Exception {
        String token = login("faculty@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/test/faculty")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void domainLeadCanAccessAssignedDomainOnly() throws Exception {
        Long backendDomainId = findDomainId("Backend");
        Long frontendDomainId = findDomainId("Frontend");

        String backendLeadToken = login("lead.backend@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/test/domain")
                        .param("domainId", String.valueOf(backendDomainId))
                        .header("Authorization", "Bearer " + backendLeadToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/test/domain")
                        .param("domainId", String.valueOf(frontendDomainId))
                        .header("Authorization", "Bearer " + backendLeadToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminCanAccessSystemEndpoint() throws Exception {
        String token = login("superadmin@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/test/system")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserWithInsufficientRoleIsForbidden() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/test/system")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void domainAccessIsValidatedAgainstDatabaseAssignments() throws Exception {
        String leadFrontendToken = login("lead.frontend@kernel.ac.in", "Kernel@123");
        Long backendDomainId = findDomainId("Backend");
        Long frontendDomainId = findDomainId("Frontend");

        mockMvc.perform(get("/api/v1/test/domain")
                        .param("domainId", String.valueOf(frontendDomainId))
                        .header("Authorization", "Bearer " + leadFrontendToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/test/domain")
                        .param("domainId", String.valueOf(backendDomainId))
                        .header("Authorization", "Bearer " + leadFrontendToken))
                .andExpect(status().isForbidden());
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

    private Long findDomainId(String domainName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM domains WHERE name = ?",
                Long.class,
                domainName
        );
    }
}
