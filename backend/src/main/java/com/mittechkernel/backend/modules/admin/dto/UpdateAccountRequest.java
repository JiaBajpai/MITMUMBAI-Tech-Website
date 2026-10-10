package com.mittechkernel.backend.modules.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Email @Size(max = 255) String email,
        @Size(min = 1, max = 100) String name,
        String program,
        Boolean active
) {
}
