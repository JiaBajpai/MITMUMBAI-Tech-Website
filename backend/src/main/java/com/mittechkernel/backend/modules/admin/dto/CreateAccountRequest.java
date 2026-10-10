package com.mittechkernel.backend.modules.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateAccountRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String name,
        @Pattern(regexp = "TECHNICAL|FOUNDATION") String program,
        @NotEmpty Set<String> roles,
        Set<Long> domainIds
) {
}
