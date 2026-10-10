package com.mittechkernel.backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ProjectRequest(
        @NotBlank(message = "Project name is required")
        String name,

        @NotNull(message = "domainId is required")
        Long domainId,

        String program,

        @NotBlank(message = "problem is required")
        String problem,

        @NotBlank(message = "solution is required")
        String solution,

        List<String> technologies,

        List<String> requiredSkills,

        @NotNull(message = "expectedMembers is required")
        @Positive(message = "expectedMembers must be greater than zero")
        Integer expectedMembers,

        String outcome,

        String status,
        String reviewComment
) {
}
