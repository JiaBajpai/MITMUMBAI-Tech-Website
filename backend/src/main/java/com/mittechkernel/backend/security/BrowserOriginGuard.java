package com.mittechkernel.backend.security;

import com.mittechkernel.backend.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BrowserOriginGuard {
    private final List<String> allowedOrigins;

    public BrowserOriginGuard(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins.stream().map(String::trim).toList();
    }

    public void requireAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || !allowedOrigins.contains(origin)) {
            throw new ForbiddenException("A valid browser origin is required for this authentication action");
        }
    }
}
