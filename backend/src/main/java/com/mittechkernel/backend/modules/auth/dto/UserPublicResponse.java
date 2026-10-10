package com.mittechkernel.backend.modules.auth.dto;

import java.util.Set;

public record UserPublicResponse(
        Long id,
        String name,
        String email,
        String program,
        Set<String> roles
) {
}
