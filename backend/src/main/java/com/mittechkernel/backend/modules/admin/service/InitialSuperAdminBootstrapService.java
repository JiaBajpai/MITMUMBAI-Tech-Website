package com.mittechkernel.backend.modules.admin.service;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.CharBuffer;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class InitialSuperAdminBootstrapService {
    public static final String INITIAL_ADMIN_EMAIL = "MITTechKernel@mitmumbai.com";
    private static final String INITIAL_ADMIN_NAME = "MIT TECH KERNEL Administrator";
    private static final long ADVISORY_LOCK_KEY = 0x4B45524E454C4144L;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogWriter auditLogWriter;
    private final String bootstrapEmail;

    @Autowired
    public InitialSuperAdminBootstrapService(JdbcTemplate jdbc,
                                             PasswordEncoder passwordEncoder,
                                             AuditLogWriter auditLogWriter) {
        this(jdbc, passwordEncoder, auditLogWriter, INITIAL_ADMIN_EMAIL);
    }

    InitialSuperAdminBootstrapService(JdbcTemplate jdbc,
                                      PasswordEncoder passwordEncoder,
                                      AuditLogWriter auditLogWriter,
                                      String bootstrapEmail) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.auditLogWriter = auditLogWriter;
        this.bootstrapEmail = bootstrapEmail;
    }

    @Transactional(readOnly = true)
    public void assertProvisionable() {
        validateConfiguredEmail(bootstrapEmail);
        refuseForExistingAccount();
        refuseForExistingSuperAdmin();
    }

    @Transactional
    public long provision(char[] password) {
        validateConfiguredEmail(bootstrapEmail);
        validatePassword(password);
        acquireBootstrapLock();
        refuseForExistingAccount();
        refuseForExistingSuperAdmin();

        String encodedPassword = passwordEncoder.encode(CharBuffer.wrap(password));
        Long userId = jdbc.queryForObject("""
                        INSERT INTO users (email, name, password_hash, program, active, password_setup_required)
                        VALUES (?, ?, ?, 'TECHNICAL', TRUE, FALSE)
                        RETURNING id
                        """, Long.class, bootstrapEmail, INITIAL_ADMIN_NAME, encodedPassword);
        if (userId == null) throw new IllegalStateException("Bootstrap did not create an account id");
        jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'SUPER_ADMIN')", userId);
        auditLogWriter.record(null, "INITIAL_SUPER_ADMIN_BOOTSTRAP", userId,
                java.util.Map.of("role", "SUPER_ADMIN", "source", "explicit-operator-command"));
        return userId;
    }

    private void refuseForExistingAccount() {
        List<ExistingAccount> matches = jdbc.query(
                "SELECT id, active FROM users WHERE lower(email) = lower(?)",
                (rs, row) -> new ExistingAccount(rs.getLong("id"), rs.getBoolean("active")),
                bootstrapEmail);
        if (!matches.isEmpty()) {
            ExistingAccount existing = matches.getFirst();
            List<String> roles = jdbc.queryForList(
                    "SELECT role FROM user_roles WHERE user_id = ? ORDER BY role", String.class, existing.id());
            throw new IllegalStateException("The requested email already belongs to account id=" + existing.id()
                    + " (active=" + existing.active() + ", roles=" + roles + "). Do not create or elevate it automatically; "
                    + "use an existing Super Admin or follow the documented controlled recovery procedure.");
        }
    }

    private void refuseForExistingSuperAdmin() {
        Integer adminCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE role = 'SUPER_ADMIN'", Integer.class);
        if (adminCount != null && adminCount > 0) {
            throw new IllegalStateException("A SUPER_ADMIN already exists. Bootstrap refused; recover access to the existing "
                    + "administrator instead of creating another one.");
        }
    }

    public static void validateConfiguredEmail(String email) {
        if (email == null || email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalStateException("Configured initial administrator email is invalid");
        }
    }

    public static void validatePassword(char[] password) {
        if (password == null || password.length < 12 || password.length > 128) {
            throw new IllegalArgumentException("Password must be between 12 and 128 characters");
        }
        boolean hasNonWhitespace = false;
        for (char value : password) {
            if (!Character.isWhitespace(value)) {
                hasNonWhitespace = true;
                break;
            }
        }
        if (!hasNonWhitespace) throw new IllegalArgumentException("Password must not be blank");
    }

    private void acquireBootstrapLock() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, ADVISORY_LOCK_KEY);
                statement.execute();
            }
            return null;
        });
    }

    private record ExistingAccount(long id, boolean active) {}
}
