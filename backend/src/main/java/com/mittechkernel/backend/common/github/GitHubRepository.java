package com.mittechkernel.backend.common.github;

public record GitHubRepository(
        Long id,
        String ownerLogin,
        String name,
        String fullName,
        String htmlUrl,
        String visibility
) {
}
