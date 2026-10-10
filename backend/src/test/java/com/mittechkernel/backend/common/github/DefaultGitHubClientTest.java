package com.mittechkernel.backend.common.github;

import com.mittechkernel.backend.common.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpStatus;

class DefaultGitHubClientTest {

    @Test
    void exchangesCodeForJsonTokenAtConfiguredEndpointWithoutReturningOtherCredentials() {
        GitHubProperties properties = properties();
        RestClient.Builder oauthBuilder = RestClient.builder().baseUrl(properties.getOauthTokenUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(oauthBuilder).build();
        server.expect(requestTo(properties.getOauthTokenUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Accept", "application/json"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=test-client")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code_verifier=pkce-verifier")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fgithub%2Foauth%2Fcallback")))
                .andRespond(withSuccess("{\"access_token\":\"opaque-token\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));

        DefaultGitHubClient client = new DefaultGitHubClient(RestClient.create("https://api.github.test"),
                oauthBuilder.build(), properties);

        assertThat(client.exchangeCodeForAccessToken("one-time-code", properties.getRedirectUri(), "pkce-verifier")).isEqualTo("opaque-token");
        server.verify();
    }

    @Test
    void repositoryPermissionErrorsReturnSafeActionableApiErrors() {
        GitHubProperties properties = properties();
        RestClient.Builder apiBuilder = RestClient.builder().baseUrl(properties.getApiUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(apiBuilder).build();
        server.expect(requestTo("https://api.github.test/user/repos?per_page=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("insufficient scope"));

        DefaultGitHubClient client = new DefaultGitHubClient(apiBuilder.build(), RestClient.create(properties.getOauthTokenUrl()), properties);

        assertThatThrownBy(() -> client.listAccessibleRepositories("opaque-token"))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("Confirm the GitHub authorization and organization access policy")
                .hasMessageNotContaining("opaque-token");
        server.verify();
    }

    private GitHubProperties properties() {
        GitHubProperties properties = new GitHubProperties();
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.setRedirectUri("http://localhost:8080/api/v1/github/oauth/callback");
        properties.setOauthTokenUrl("https://github.com/login/oauth/access_token");
        properties.setApiUrl("https://api.github.test");
        return properties;
    }
}
