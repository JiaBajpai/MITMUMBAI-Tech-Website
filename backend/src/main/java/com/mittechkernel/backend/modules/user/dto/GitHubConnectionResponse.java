package com.mittechkernel.backend.modules.user.dto;

public record GitHubConnectionResponse(
        String provider,
        String state,
        String authorizationUrl
) {
}
