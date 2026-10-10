package com.mittechkernel.backend.modules.admin.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailablePasswordSetupDelivery implements PasswordSetupDelivery {
    @Override public boolean isAvailable() { return false; }
    @Override public void send(String email, String name, String link) {
        throw new IllegalStateException("Password setup delivery is not configured");
    }
}
