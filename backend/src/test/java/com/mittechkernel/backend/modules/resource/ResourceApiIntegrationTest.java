package com.mittechkernel.backend.modules.resource;

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
class ResourceApiIntegrationTest {

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
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanListResources() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").exists());
    }

    @Test
    void authenticatedMemberCanFilterPublicResourcesByDomain() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        Long backendDomainId = findDomainId("Backend");

        mockMvc.perform(get("/api/v1/resources").param("domainId", backendDomainId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].domainId").value(backendDomainId));
    }

    @Test
    void authenticatedUserCanRetrieveResource() throws Exception {
        Long resourceId = findFirstResourceId();
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/resources/{id}", resourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(resourceId));
    }

    @Test
    void invalidResourceIdReturns404() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/resources/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFilterReturnsBadRequest() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/resources")
                        .param("type", "INVALID_TYPE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void domainLeadCanAccessOnlyAssignedDomainResources() throws Exception {
        Long backendDomainId = findDomainId("Backend");
        Long frontendDomainId = findDomainId("Frontend");
        Long frontendResourceId = findResourceIdByDomain(frontendDomainId);
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/resources")
                        .param("domainId", String.valueOf(backendDomainId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/resources")
                        .param("domainId", String.valueOf(frontendDomainId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/resources/{id}", frontendResourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void validDomainAndResourceRelationshipWorks() throws Exception {
        Long backendDomainId = findDomainId("Backend");
        Long backendResourceId = findResourceIdByDomain(backendDomainId);
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/resources")
                        .param("domainId", String.valueOf(backendDomainId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].domainId").value(backendDomainId));

        mockMvc.perform(get("/api/v1/resources/{id}", backendResourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.domainId").value(backendDomainId));
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

    private Long findFirstResourceId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM resources ORDER BY created_at DESC LIMIT 1",
                Long.class
        );
    }

    private Long findDomainId(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM domains WHERE name = ?",
                Long.class,
                name
        );
    }

    private Long findResourceIdByDomain(Long domainId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM resources WHERE domain_id = ? ORDER BY created_at DESC LIMIT 1",
                Long.class,
                domainId
        );
    }
}
