package com.mittechkernel.backend.modules.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskCompletionRequest(
        @NotBlank(message = "repoUrl is required")
        String repoUrl,
        String commitSha,
        String notes
) {
}
