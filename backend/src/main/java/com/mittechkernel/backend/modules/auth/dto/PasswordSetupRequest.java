package com.mittechkernel.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordSetupRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters") String password
) {
}
