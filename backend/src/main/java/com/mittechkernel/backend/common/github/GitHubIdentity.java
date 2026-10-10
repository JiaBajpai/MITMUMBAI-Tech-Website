package com.mittechkernel.backend.common.github;

public record GitHubIdentity(
        Long id,
        String login,
        String name,
        String email,
        String avatarUrl,
        String htmlUrl
) {
}
