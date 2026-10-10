package com.mittechkernel.backend.common.config;

import com.mittechkernel.backend.common.github.GitHubClient;
import com.mittechkernel.backend.common.github.GitHubProperties;
import com.mittechkernel.backend.common.github.DefaultGitHubClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubClientConfig {

    @Bean
    public RestClient gitHubRestClient(GitHubProperties gitHubProperties) {
        return RestClient.builder()
                .baseUrl(gitHubProperties.getApiUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Bean
    public GitHubClient gitHubClient(RestClient gitHubRestClient, GitHubProperties gitHubProperties) {
        RestClient oauthRestClient = RestClient.builder()
                .baseUrl(gitHubProperties.getOauthTokenUrl())
                .build();
        return new DefaultGitHubClient(gitHubRestClient, oauthRestClient, gitHubProperties);
    }
}
