package com.mittechkernel.backend.modules.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record TaskRequest(
        @NotNull(message = "sessionId is required")
        Long sessionId,

        Long projectId,

        @NotBlank(message = "title is required")
        String title,

        List<String> requirements,
        Instant deadline,
        Boolean verificationRequired,
        Long assigneeId,
        String status
) {
}
