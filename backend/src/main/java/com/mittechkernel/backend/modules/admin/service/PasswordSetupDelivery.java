package com.mittechkernel.backend.modules.admin.service;

public interface PasswordSetupDelivery {
    boolean isAvailable();
    void send(String email, String name, String link);
}
