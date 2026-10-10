package com.mittechkernel.backend.modules.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.github.client-id=integration-client-id",
        "app.github.client-secret=integration-client-secret",
        "app.github.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class GitHubFoundationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void unauthenticatedGitHubConnectRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(post("/api/v1/me/github/connect"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanInitiateGitHubConnection() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        String response = mockMvc.perform(post("/api/v1/me/github/connect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizationUrl").exists())
                .andExpect(jsonPath("$.data.state").exists())
                .andExpect(jsonPath("$.data.provider").value("GITHUB"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        String authorizationUrl = root.path("data").path("authorizationUrl").asText();
        String state = root.path("data").path("state").asText();
        var query = UriComponentsBuilder.fromUriString(authorizationUrl).build().getQueryParams();

        assertThat(authorizationUrl).startsWith("https://github.com/login/oauth/authorize?");
        assertThat(query.getFirst("client_id")).isEqualTo("integration-client-id");
        assertThat(query.getFirst("redirect_uri")).isEqualTo("http://localhost:8080/api/v1/github/oauth/callback");
        assertThat(query.getFirst("scope")).isEqualTo("read:user");
        assertThat(query.getFirst("state")).isEqualTo(state);
        assertThat(authorizationUrl).doesNotContain("client_secret");
        assertThat(state).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void invalidGitHubCallbackStateIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/github/oauth/callback")
                        .param("code", "test-code")
                        .param("state", "bad-state"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void githubCredentialsAreNeverExposedInResponses() throws Exception {
        String token = login("student@kernel.ac.in", "Kernel@123");

        String response = mockMvc.perform(post("/api/v1/me/github/connect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("client_secret");
        assertThat(response).doesNotContain("token");
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
}
