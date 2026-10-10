package com.mittechkernel.backend.modules.user.dto;

import java.util.Set;

public record UserProfileResponse(
        Long userId,
        String name,
        String email,
        String program,
        Set<String> roles,
        String bio,
        String avatarUrl,
        String githubUrl,
        String linkedinUrl,
        String phone
) {
}
