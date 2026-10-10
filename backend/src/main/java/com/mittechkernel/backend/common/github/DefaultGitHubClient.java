package com.mittechkernel.backend.common.github;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.mittechkernel.backend.common.exception.UpstreamServiceException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultGitHubClient implements GitHubClient {

    private final RestClient restClient;
    private final RestClient oauthRestClient;
    private final GitHubProperties gitHubProperties;

    public DefaultGitHubClient(RestClient restClient, RestClient oauthRestClient, GitHubProperties gitHubProperties) {
        this.restClient = restClient;
        this.oauthRestClient = oauthRestClient;
        this.gitHubProperties = gitHubProperties;
    }

    @Override
    public String exchangeCodeForAccessToken(String code, String redirectUri, String codeVerifier) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("GitHub authorization code is required");
        }
        if (redirectUri == null || redirectUri.isBlank() || codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("GitHub redirect URI and PKCE verifier are required");
        }

        try {
            String body = String.format(
                    "client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&code_verifier=%s",
                    urlEncode(gitHubProperties.getClientId()),
                    urlEncode(gitHubProperties.getClientSecret()),
                    urlEncode(code),
                    urlEncode(redirectUri),
                    urlEncode(codeVerifier)
            );

            ResponseEntity<Map> response = oauthRestClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            Map payload = response.getBody();
            if (payload == null || payload.get("access_token") == null) {
                throw new IllegalStateException("GitHub OAuth token exchange failed");
            }
            return payload.get("access_token").toString();
        } catch (RestClientException ex) {
            throw new UpstreamServiceException("GitHub authorization could not be completed. Verify the OAuth app credentials and callback URL.");
        }
    }

    @Override
    public GitHubIdentity getAuthenticatedUser(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token is required");
        }

        try {
            Map<String, Object> payload = restClient.get()
                    .uri("/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (payload == null) {
                throw new IllegalStateException("GitHub user profile lookup failed");
            }

            return new GitHubIdentity(
                    payload.get("id") instanceof Number n ? n.longValue() : null,
                    payload.get("login") == null ? null : payload.get("login").toString(),
                    payload.get("name") == null ? null : payload.get("name").toString(),
                    payload.get("email") == null ? null : payload.get("email").toString(),
                    payload.get("avatar_url") == null ? null : payload.get("avatar_url").toString(),
                    payload.get("html_url") == null ? null : payload.get("html_url").toString()
            );
        } catch (RestClientException ex) {
            throw new UpstreamServiceException("GitHub account details could not be retrieved.");
        }
    }

    @Override
    public List<GitHubRepository> listAccessibleRepositories(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token is required");
        }

        try {
            List<Map<String, Object>> payload = restClient.get()
                    .uri("/user/repos?per_page=100")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(List.class);

            if (payload == null || payload.isEmpty()) {
                return List.of();
            }

            List<GitHubRepository> repositories = new ArrayList<>();
            for (Map<String, Object> repository : payload) {
                repositories.add(toGitHubRepository(repository));
            }
            return repositories;
        } catch (RestClientException ex) {
            throw new UpstreamServiceException(
                    "GitHub could not return accessible repositories. Confirm the GitHub authorization and organization access policy.");
        }
    }

    @Override
    public GitHubRepository getRepository(String accessToken, Long repositoryId) {
        if (repositoryId == null) {
            throw new IllegalArgumentException("GitHub repository ID is required");
        }

        return listAccessibleRepositories(accessToken).stream()
                .filter(repository -> repository.id().equals(repositoryId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<GitHubCommit> listCommits(String accessToken, String owner, String repository) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token is required");
        }
        if (owner == null || owner.isBlank() || repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("GitHub repository owner and name are required");
        }
        try {
            List<Map<String, Object>> payload = restClient.get()
                    .uri("/repos/{owner}/{repo}/commits?per_page=100&page=1", owner, repository)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(List.class);
            if (payload == null || payload.isEmpty()) {
                return List.of();
            }
            List<GitHubCommit> commits = new ArrayList<>();
            for (Map<String, Object> item : payload) {
                Map<String, Object> commit = item.get("commit") instanceof Map<?, ?> value
                        ? (Map<String, Object>) value : Map.of();
                Map<String, Object> authorData = commit.get("author") instanceof Map<?, ?> value
                        ? (Map<String, Object>) value : Map.of();
                Map<String, Object> author = item.get("author") instanceof Map<?, ?> value
                        ? (Map<String, Object>) value : Map.of();
                Object dateValue = authorData.get("date");
                if (item.get("sha") == null || item.get("html_url") == null || dateValue == null) {
                    continue;
                }
                commits.add(new GitHubCommit(
                        item.get("sha").toString(),
                        item.get("html_url").toString(),
                        commit.get("message") == null ? "" : commit.get("message").toString(),
                        Instant.parse(dateValue.toString()),
                        author.get("id") instanceof Number number ? number.longValue() : null,
                        author.get("login") == null ? null : author.get("login").toString()
                ));
            }
            return List.copyOf(commits);
        } catch (RestClientException ex) {
            throw new IllegalStateException("GitHub commit lookup failed", ex);
        }
    }

    private GitHubRepository toGitHubRepository(Map<String, Object> repository) {
        Map<String, Object> owner = repository.get("owner") instanceof Map<?, ?> ownerMap ? (Map<String, Object>) ownerMap : Map.of();
        String ownerLogin = owner.get("login") == null ? null : owner.get("login").toString();
        String fullName = repository.get("full_name") == null ? null : repository.get("full_name").toString();
        String name = repository.get("name") == null ? null : repository.get("name").toString();
        String htmlUrl = repository.get("html_url") == null ? null : repository.get("html_url").toString();
        Boolean isPrivate = repository.get("private") instanceof Boolean value ? value : false;

        return new GitHubRepository(
                repository.get("id") instanceof Number n ? n.longValue() : null,
                ownerLogin,
                name,
                fullName,
                htmlUrl,
                isPrivate ? "PRIVATE" : "PUBLIC"
        );
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
