package com.mittechkernel.backend.modules.auth.dto;

public record AuthSessionResponse(String accessToken, UserPublicResponse user) {
}
