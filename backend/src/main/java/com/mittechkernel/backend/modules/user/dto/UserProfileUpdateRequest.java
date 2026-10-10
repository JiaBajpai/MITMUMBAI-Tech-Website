package com.mittechkernel.backend.modules.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 500, message = "bio must be at most 500 characters")
        String bio,

        @Size(max = 500, message = "avatarUrl must be at most 500 characters")
        String avatarUrl,

        @Size(max = 255, message = "githubUrl must be at most 255 characters")
        String githubUrl,

        @Size(max = 255, message = "linkedinUrl must be at most 255 characters")
        String linkedinUrl,

        @Pattern(regexp = "^(\\+?[0-9\\s-]{7,20})?$", message = "phone must be a valid phone number")
        String phone
) {
}
