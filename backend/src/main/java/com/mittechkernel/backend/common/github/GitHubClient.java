package com.mittechkernel.backend.common.github;

import java.util.List;

public interface GitHubClient {

    String exchangeCodeForAccessToken(String code, String redirectUri, String codeVerifier);

    GitHubIdentity getAuthenticatedUser(String accessToken);

    List<GitHubRepository> listAccessibleRepositories(String accessToken);

    GitHubRepository getRepository(String accessToken, Long repositoryId);

    java.util.List<GitHubCommit> listCommits(String accessToken, String owner, String repository);
}
