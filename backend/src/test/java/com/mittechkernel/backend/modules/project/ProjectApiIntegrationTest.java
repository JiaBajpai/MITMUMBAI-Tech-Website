package com.mittechkernel.backend.modules.project;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ProjectApiIntegrationTest {

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
    void unauthenticatedProjectAccessIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanListProjects() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    @Test
    void projectNotFoundReturns404() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/projects/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void projectCreatorIsAssignedFromSecurityContext() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("Backend");

        String body = "{\"name\":\"API Test Project\",\"domainId\":" + domainId + ",\"problem\":\"Need a project\",\"solution\":\"Build it\",\"technologies\":[\"Java\"],\"requiredSkills\":[\"REST\"],\"expectedMembers\":3}";

        String response = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        Long createdBy = root.path("data").path("createdBy").asLong();
        Long expectedUserId = findUserId("student@kernel.ac.in");

        org.junit.jupiter.api.Assertions.assertEquals(expectedUserId, createdBy);
    }

    @Test
    void domainLeadCannotCreateProjectOutsideAssignedDomain() throws Exception {
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("Frontend");

        String body = "{\"name\":\"Blocked Domain Project\",\"domainId\":" + domainId + ",\"problem\":\"Need a project\",\"solution\":\"Build it\",\"technologies\":[\"React\"],\"requiredSkills\":[\"UI\"],\"expectedMembers\":2}";

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectUpdateRequiresOwnershipOrPrivilege() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("Backend");
        Long projectId = createProjectForTest("Owned Update Project", domainId, token);

        String body = "{\"name\":\"Owned Update Project Renamed\"}";

        mockMvc.perform(patch("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String otherToken = login("faculty@kernel.ac.in", "Kernel@123");
        mockMvc.perform(patch("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectOwnerCannotMoveProjectIntoAnotherProgram() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        Long projectId = createProjectForTest("Program Alignment Update Project", findDomainId("Backend"), token);

        mockMvc.perform(patch("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"program\":\"FOUNDATION\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectOwnerCannotActivateAProposalWithoutReviewerApproval() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long projectId = createProjectForTest("Owner Approval Bypass", findDomainId("Backend"), ownerToken);

        mockMvc.perform(patch("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM projects WHERE id = ?", String.class, projectId)).isEqualTo("PROPOSED");
    }

    @Test
    void invalidProjectUpdateIsRejected() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("Backend");
        Long projectId = createProjectForTest("Invalid Update Project", domainId, token);

        String body = "{\"expectedMembers\":0}";

        mockMvc.perform(patch("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joiningProjectCreatesRequestMembership() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("Backend");
        Long projectId = createProjectForTest("Join Request Project", domainId, ownerToken);
        String token = login("faculty@kernel.ac.in", "Kernel@123");

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(findUserId("faculty@kernel.ac.in")))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));
    }

    @Test
    void duplicateMembershipIsRejected() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("Backend");
        Long projectId = createProjectForTest("Duplicate Join Project", domainId, ownerToken);
        String token = login("faculty@kernel.ac.in", "Kernel@123");

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthorizedMembershipApprovalIsRejected() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("Backend");
        Long projectId = createProjectForTest("Approval Permission Project", domainId, ownerToken);
        String joiningToken = login("core@kernel.ac.in", "Kernel@123");
        String outsiderToken = login("faculty@kernel.ac.in", "Kernel@123");

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + joiningToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}/approve", projectId, findUserId("core@kernel.ac.in"))
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void domainLeadCanManagePermittedProject() throws Exception {
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");
        Long domainId = findDomainId("AI/ML/IoT");
        Long projectId = createProjectForTest("Lead Managed AI Project", domainId, login("student@kernel.ac.in", "Kernel@123"));

        String body = "{\"status\":\"ACTIVE\"}";

        mockMvc.perform(patch("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void domainLeadCannotManageProjectOutsideAssignedDomain() throws Exception {
        String token = login("lead.backend@kernel.ac.in", "Kernel@123");
        Long projectId = findProjectIdByName("Portfolio Reviewer");

        String body = "{\"status\":\"PAUSED\"}";

        mockMvc.perform(patch("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectOwnerCanRejectPendingMembership() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long projectId = createProjectForTest("Membership Reject Project", findDomainId("Backend"), ownerToken);
        String joiningToken = login("faculty@kernel.ac.in", "Kernel@123");
        Long joiningUserId = findUserId("faculty@kernel.ac.in");

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + joiningToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}/reject", projectId, joiningUserId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(joiningUserId))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void selfApprovalIsRejected() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long projectId = createProjectForTest("Self Approve Project", findDomainId("Backend"), ownerToken);
        String joiningToken = login("core@kernel.ac.in", "Kernel@123");
        Long joiningUserId = findUserId("core@kernel.ac.in");

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + joiningToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}/approve", projectId, joiningUserId)
                        .header("Authorization", "Bearer " + joiningToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanLeaveProject() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long projectId = createProjectForTest("Member Leave Project", findDomainId("Backend"), ownerToken);
        String memberToken = login("faculty@kernel.ac.in", "Kernel@123");
        Long memberUserId = findUserId("faculty@kernel.ac.in");

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}/approve", projectId, memberUserId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/projects/{id}/leave", projectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void projectOwnerCannotLeaveProject() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long projectId = createProjectForTest("Owner Leave Project", findDomainId("Backend"), ownerToken);

        mockMvc.perform(delete("/api/v1/projects/{id}/leave", projectId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repeatedApprovalIsRejected() throws Exception {
        String ownerToken = login("student@kernel.ac.in", "Kernel@123");
        Long projectId = createProjectForTest("Repeated Approval Project", findDomainId("Backend"), ownerToken);
        String joiningToken = login("faculty@kernel.ac.in", "Kernel@123");
        Long joiningUserId = findUserId("faculty@kernel.ac.in");

        mockMvc.perform(post("/api/v1/projects/{id}/join", projectId)
                        .header("Authorization", "Bearer " + joiningToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}/approve", projectId, joiningUserId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}/approve", projectId, joiningUserId)
                        .header("Authorization", "Bearer " + ownerToken))
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

    private Long findDomainId(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM domains WHERE name = ?",
                Long.class,
                name
        );
    }

    private Long findUserId(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }

    private Long findProjectIdByName(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM projects WHERE name = ?",
                Long.class,
                name
        );
    }

    private Long createProjectForTest(String name, Long domainId, String token) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"domainId\":" + domainId + ",\"problem\":\"Need a project\",\"solution\":\"Build it\",\"technologies\":[\"Java\"],\"requiredSkills\":[\"REST\"],\"expectedMembers\":3}";

        String response = mockMvc.perform(post("/api/v1/projects")
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
}
