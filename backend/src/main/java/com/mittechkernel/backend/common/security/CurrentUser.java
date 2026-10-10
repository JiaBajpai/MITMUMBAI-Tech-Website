package com.mittechkernel.backend.common.security;

import java.util.Set;

public record CurrentUser(Long id, String email, Set<String> roles) {
}
