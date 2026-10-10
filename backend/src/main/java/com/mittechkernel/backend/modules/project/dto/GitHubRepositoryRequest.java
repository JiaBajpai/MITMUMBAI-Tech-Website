package com.mittechkernel.backend.modules.project.dto;

import jakarta.validation.constraints.NotNull;

public record GitHubRepositoryRequest(
        @NotNull(message = "githubRepoId is required") Long githubRepoId
) {
}
