package com.mittechkernel.backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectReviewRequest(
        @NotBlank(message = "decision is required")
        String decision,

        String comment
) {
}
