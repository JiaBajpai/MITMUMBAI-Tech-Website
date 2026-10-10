package com.mittechkernel.backend.modules.user.dto;

public record GitHubConnectionStatusResponse(
        String provider,
        boolean connected,
        String username
) {
}
