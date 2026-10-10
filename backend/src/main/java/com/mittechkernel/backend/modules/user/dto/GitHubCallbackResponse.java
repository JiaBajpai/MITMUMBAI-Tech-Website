package com.mittechkernel.backend.modules.user.dto;

public record GitHubCallbackResponse(
        String provider,
        String status
) {
}
