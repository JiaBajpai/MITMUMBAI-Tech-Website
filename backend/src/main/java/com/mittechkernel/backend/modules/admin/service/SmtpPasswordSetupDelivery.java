package com.mittechkernel.backend.modules.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpPasswordSetupDelivery implements PasswordSetupDelivery {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordSetupDelivery(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override public boolean isAvailable() { return true; }

    @Override
    public void send(String email, String name, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Set your MIT Tech Kernel account password");
        message.setText("Hello " + name + ",\n\nUse this one-time link to set or reset your password. It expires shortly and can only be used once:\n\n"
                + link + "\n\nIf you did not expect this message, contact the MIT Tech Kernel administrators.");
        mailSender.send(message);
    }
}
