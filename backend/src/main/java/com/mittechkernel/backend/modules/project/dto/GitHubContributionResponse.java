package com.mittechkernel.backend.modules.project.dto;

import java.time.Instant;

public record GitHubContributionResponse(
        Long id,
        String sha,
        String message,
        String url,
        String authorLogin,
        Instant committedAt,
        Long kernelUserId,
        boolean verified,
        Instant verifiedAt
) {
}
