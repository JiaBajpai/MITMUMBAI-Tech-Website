package com.mittechkernel.backend.modules.auth.dto;

public record AuthSuccessResponse(
        String accessToken,
        String refreshToken,
        UserPublicResponse user
) {
}
