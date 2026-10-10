package com.mittechkernel.backend.common.github;

import java.time.Instant;

public record GitHubCommit(
        String sha,
        String url,
        String message,
        Instant committedAt,
        Long authorId,
        String authorLogin
) {
}
