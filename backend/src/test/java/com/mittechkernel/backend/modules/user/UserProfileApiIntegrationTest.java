package com.mittechkernel.backend.modules.user;

import com.mittechkernel.backend.modules.user.entity.UserAccount;
import com.mittechkernel.backend.modules.user.entity.UserProfile;
import com.mittechkernel.backend.modules.user.repository.UserAccountRepository;
import com.mittechkernel.backend.modules.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserProfileApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        resetProfile("student@kernel.ac.in", "Backend + CP, loves shipping side projects",
                "https://github.com/alex-thomas", null, null);
        resetProfile("faculty@kernel.ac.in", null, null, null, null);
    }

    @Test
    void unauthenticatedUserProfileRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanRetrieveOwnProfile() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(6))
                .andExpect(jsonPath("$.data.email").value("student@kernel.ac.in"))
                .andExpect(jsonPath("$.data.name").value("Alex Thomas"))
                .andExpect(jsonPath("$.data.bio").value("Backend + CP, loves shipping side projects"));
    }

    @Test
    void authenticatedUserCanUpdateAllowedProfileFields() throws Exception {
        String token = login("faculty@kernel.ac.in", "Kernel@123");
        String body = "{\"bio\":\"Updated bio for the club\",\"githubUrl\":\"https://github.com/alex-thomas\",\"linkedinUrl\":\"https://linkedin.com/in/alex-thomas\",\"phone\":\"+1-555-0102\"}";

        mockMvc.perform(patch("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Updated bio for the club"))
                .andExpect(jsonPath("$.data.githubUrl").value("https://github.com/alex-thomas"))
                .andExpect(jsonPath("$.data.linkedinUrl").value("https://linkedin.com/in/alex-thomas"))
                .andExpect(jsonPath("$.data.phone").value("+1-555-0102"))
                .andExpect(jsonPath("$.data.roles[0]").value("FACULTY"));
    }

    @Test
    void protectedAuthorizationFieldsCannotBeChangedViaProfileUpdate() throws Exception {
        String token = login("faculty@kernel.ac.in", "Kernel@123");
        String body = "{\"roles\":[\"SUPER_ADMIN\"],\"email\":\"hacker@kernel.ac.in\",\"program\":\"FOUNDATION\"}";

        mockMvc.perform(patch("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("faculty@kernel.ac.in"))
                .andExpect(jsonPath("$.data.program").value("TECHNICAL"))
                .andExpect(jsonPath("$.data.roles[0]").value("FACULTY"));
    }

    @Test
    void invalidProfileUpdateReturnsBadRequest() throws Exception {
        String token = login("faculty@kernel.ac.in", "Kernel@123");
        String body = "{\"bio\":\"" + "x".repeat(600) + "\"}";

        mockMvc.perform(patch("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("accessToken").asText();
    }

    private void resetProfile(String email, String bio, String githubUrl, String linkedinUrl, String phone) {
        Long userId = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed user not found: " + email))
                .getId();

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUserId(userId);
                    newProfile.setCreatedAt(Instant.now());
                    return newProfile;
                });

        profile.setBio(bio);
        profile.setGithubUrl(githubUrl);
        profile.setLinkedinUrl(linkedinUrl);
        profile.setPhone(phone);
        profile.setAvatarUrl(null);
        profile.setUpdatedAt(Instant.now());
        userProfileRepository.save(profile);
    }
}
