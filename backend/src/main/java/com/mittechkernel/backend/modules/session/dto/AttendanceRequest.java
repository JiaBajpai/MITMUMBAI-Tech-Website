package com.mittechkernel.backend.modules.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(
        @NotNull(message = "userId is required")
        Long userId,

        @NotBlank(message = "status is required")
        String status
) {
}
