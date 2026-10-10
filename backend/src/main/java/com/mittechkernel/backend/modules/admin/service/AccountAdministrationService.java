package com.mittechkernel.backend.modules.admin.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.DeliveryUnavailableException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.admin.dto.AccountResponse;
import com.mittechkernel.backend.modules.admin.dto.CreateAccountRequest;
import com.mittechkernel.backend.modules.admin.dto.UpdateAccountRequest;
import com.mittechkernel.backend.modules.admin.dto.UpdateRolesRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.keygen.KeyGenerators;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountAdministrationService {
    private static final Set<String> VALID_ROLES = Set.of("SUPER_ADMIN", "CORE_MEMBER", "FACULTY", "DOMAIN_LEAD", "STUDENT");
    private final JdbcTemplate jdbc;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordSetupDelivery delivery;
    private final AuditLogWriter audit;
    private final String frontendBaseUrl;

    public AccountAdministrationService(JdbcTemplate jdbc, CurrentUserService currentUserService,
                                        PasswordEncoder passwordEncoder, PasswordSetupDelivery delivery,
                                        AuditLogWriter audit,
                                        @Value("${app.mail.frontend-base-url}") String frontendBaseUrl) {
        this.jdbc = jdbc;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.delivery = delivery;
        this.audit = audit;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(String search) {
        CurrentUser actor = currentUserService.getCurrentUser();
        List<AccountResponse> accounts = jdbc.query("SELECT id, email, name, program, active FROM users ORDER BY name, id",
                (rs, row) -> account(rs.getLong("id"), rs.getString("email"), rs.getString("name"), rs.getString("program"), rs.getBoolean("active")));
        String normalized = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return accounts.stream()
                .filter(account -> isSuperAdmin(actor) || (account.roles().equals(List.of("STUDENT"))))
                .filter(account -> normalized.isEmpty() || (account.name() + " " + account.email() + " " + account.id()).toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        CurrentUser actor = currentUserService.getCurrentUser();
        requireMailDelivery();
        String email = normalizeEmail(request.email());
        if (emailExists(email)) throw new BadRequestException("An account with this email already exists");
        Set<String> roles = normalizeRoles(request.roles());
        List<Long> domainIds = request.domainIds() == null ? List.of() : request.domainIds().stream().distinct().sorted().toList();
        if (!isSuperAdmin(actor) && !roles.equals(Set.of("STUDENT"))) throw new ForbiddenException("Core Members may create student accounts only");
        if (roles.contains("DOMAIN_LEAD") != !domainIds.isEmpty()) throw new BadRequestException("Domain Lead accounts must have at least one domain assignment");
        validateDomains(domainIds);

        String randomPassword = HexFormat.of().formatHex(KeyGenerators.secureRandom(32).generateKey());
        long userId;
        try {
            userId = jdbc.queryForObject("INSERT INTO users (email, name, password_hash, program, active, password_setup_required) VALUES (?, ?, ?, ?, TRUE, TRUE) RETURNING id",
                    Long.class, email, request.name().trim(), passwordEncoder.encode(randomPassword), normalizeProgram(request.program()));
        } catch (DuplicateKeyException duplicate) {
            throw new BadRequestException("An account with this email already exists");
        }
        insertRoles(userId, roles);
        for (Long domainId : domainIds) assignDomainRow(userId, domainId, actor.id());
        issuePasswordLink(userId, "INITIAL", Duration.ofHours(24));
        audit.record(actor.id(), "ACCOUNT_CREATE", userId, Map.of("roles", roles, "program", normalizeProgram(request.program())));
        return getAccount(userId, actor);
    }

    @Transactional
    public AccountResponse update(long userId, UpdateAccountRequest request) {
        CurrentUser actor = currentUserService.getCurrentUser();
        AccountResponse existing = getAccount(userId, actor);
        if (request.email() == null && request.name() == null && request.program() == null && request.active() == null)
            throw new BadRequestException("At least one account field must be provided");
        if (!isSuperAdmin(actor) && !existing.roles().equals(List.of("STUDENT")))
            throw new ForbiddenException("Core Members may manage student accounts only");

        String email = request.email() == null ? existing.email() : normalizeEmail(request.email());
        if (!email.equals(existing.email()) && emailExists(email)) throw new BadRequestException("An account with this email already exists");
        boolean active = request.active() == null ? existing.active() : request.active();
        if (existing.roles().contains("SUPER_ADMIN") && existing.active() && !active) ensureNotLastActiveAdmin();
        String program = request.program() == null ? existing.program() : normalizeProgram(request.program());
        jdbc.update("UPDATE users SET email = ?, name = ?, program = ?, active = ?, updated_at = now() WHERE id = ?",
                email, request.name() == null ? existing.name() : request.name().trim(), program, active, userId);
        if (!active) jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
        audit.record(actor.id(), "ACCOUNT_UPDATE", userId, Map.of("emailChanged", !email.equals(existing.email()), "active", active, "program", program));
        return getAccount(userId, actor);
    }

    @Transactional
    public AccountResponse setRoles(long userId, UpdateRolesRequest request) {
        CurrentUser actor = currentUserService.getCurrentUser();
        if (!isSuperAdmin(actor)) throw new ForbiddenException("Only Super Admins may assign account roles");
        AccountResponse existing = getAccount(userId, actor);
        Set<String> next = normalizeRoles(request.roles());
        if (existing.roles().contains("SUPER_ADMIN") && existing.active() && !next.contains("SUPER_ADMIN")) ensureNotLastActiveAdmin();
        if (next.contains("DOMAIN_LEAD") && existing.domainIds().isEmpty()) throw new BadRequestException("Assign at least one domain before granting DOMAIN_LEAD");
        if (!next.contains("DOMAIN_LEAD")) jdbc.update("DELETE FROM domain_leads WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        insertRoles(userId, next);
        audit.record(actor.id(), "ROLE_ASSIGN", userId, Map.of("roles", next));
        return getAccount(userId, actor);
    }

    @Transactional
    public AccountResponse assignDomainLead(long userId, long domainId) {
        CurrentUser actor = currentUserService.getCurrentUser();
        if (!isSuperAdmin(actor)) throw new ForbiddenException("Only Super Admins may manage domain-lead assignments");
        getAccount(userId, actor);
        validateDomains(List.of(domainId));
        assignDomainRow(userId, domainId, actor.id());
        if (!jdbc.queryForList("SELECT role FROM user_roles WHERE user_id = ?", String.class, userId).contains("DOMAIN_LEAD"))
            jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'DOMAIN_LEAD')", userId);
        audit.record(actor.id(), "DOMAIN_LEAD_ASSIGN", userId, Map.of("domainId", domainId));
        return getAccount(userId, actor);
    }

    @Transactional
    public AccountResponse removeDomainLead(long userId, long domainId) {
        CurrentUser actor = currentUserService.getCurrentUser();
        if (!isSuperAdmin(actor)) throw new ForbiddenException("Only Super Admins may manage domain-lead assignments");
        getAccount(userId, actor);
        validateDomains(List.of(domainId));
        int changed = jdbc.update("DELETE FROM domain_leads WHERE user_id = ? AND domain_id = ?", userId, domainId);
        if (changed == 0) throw new ResourceNotFoundException("Domain lead assignment", domainId);
        boolean hasCurrentAssignments = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM domain_leads WHERE user_id = ? AND start_date <= CURRENT_DATE AND (end_date IS NULL OR end_date >= CURRENT_DATE))", Boolean.class, userId));
        if (!hasCurrentAssignments) jdbc.update("DELETE FROM user_roles WHERE user_id = ? AND role = 'DOMAIN_LEAD'", userId);
        audit.record(actor.id(), "DOMAIN_LEAD_REMOVE", userId, Map.of("domainId", domainId));
        return getAccount(userId, actor);
    }

    @Transactional
    public void issuePasswordReset(long userId) {
        CurrentUser actor = currentUserService.getCurrentUser();
        requireMailDelivery();
        AccountResponse account = getAccount(userId, actor);
        issuePasswordLink(userId, account.passwordSetupRequired() ? "INITIAL" : "RESET", Duration.ofMinutes(60));
        audit.record(actor.id(), "PASSWORD_RESET_ISSUED", userId, Map.of("delivery", "email"));
    }

    @Transactional
    public void completePasswordSetup(String token, String password) {
        String tokenHash = hashToken(token);
        List<Long> ids = jdbc.query("SELECT user_id FROM password_setup_tokens WHERE token_hash = ? AND expires_at > now() FOR UPDATE",
                (rs, row) -> rs.getLong(1), tokenHash);
        if (ids.isEmpty()) throw new BadRequestException("Password setup link is invalid or expired");
        long userId = ids.getFirst();
        Boolean active = jdbc.queryForObject("SELECT active FROM users WHERE id = ?", Boolean.class, userId);
        if (!Boolean.TRUE.equals(active)) throw new BadRequestException("This account is inactive");
        jdbc.update("UPDATE users SET password_hash = ?, password_setup_required = FALSE, auth_version = auth_version + 1, updated_at = now() WHERE id = ?", passwordEncoder.encode(password), userId);
        jdbc.update("DELETE FROM password_setup_tokens WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
        audit.record(null, "PASSWORD_SETUP_COMPLETED", userId, Map.of("sessionRevocation", true));
    }

    private void issuePasswordLink(long userId, String purpose, Duration duration) {
        byte[] bytes = KeyGenerators.secureRandom(32).generateKey();
        String rawToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("DELETE FROM password_setup_tokens WHERE user_id = ?", userId);
        jdbc.update("INSERT INTO password_setup_tokens (user_id, token_hash, purpose, expires_at) VALUES (?, ?, ?, now() + (? * interval '1 second'))",
                userId, hashToken(rawToken), purpose, duration.toSeconds());
        AccountResponse account = accountById(userId);
        String link = UriComponentsBuilder.fromUriString(frontendBaseUrl).path("/set-password").queryParam("token", rawToken).build().toUriString();
        try {
            delivery.send(account.email(), account.name(), link);
        } catch (RuntimeException ex) {
            throw new DeliveryUnavailableException();
        }
    }

    private AccountResponse getAccount(long userId, CurrentUser actor) {
        AccountResponse account = accountById(userId);
        if (!isSuperAdmin(actor) && !account.roles().equals(List.of("STUDENT"))) throw new ForbiddenException("Core Members may manage student accounts only");
        return account;
    }

    private AccountResponse accountById(long id) {
        List<AccountResponse> results = jdbc.query("SELECT id, email, name, program, active, password_setup_required FROM users WHERE id = ?",
                (rs, row) -> account(rs.getLong("id"), rs.getString("email"), rs.getString("name"), rs.getString("program"), rs.getBoolean("active"), rs.getBoolean("password_setup_required")), id);
        if (results.isEmpty()) throw new ResourceNotFoundException("User", id);
        return results.getFirst();
    }

    private AccountResponse account(long id, String email, String name, String program, boolean active) {
        boolean setupRequired = Boolean.TRUE.equals(jdbc.queryForObject("SELECT password_setup_required FROM users WHERE id = ?", Boolean.class, id));
        return account(id, email, name, program, active, setupRequired);
    }

    private AccountResponse account(long id, String email, String name, String program, boolean active, boolean setupRequired) {
        List<String> roles = jdbc.queryForList("SELECT role FROM user_roles WHERE user_id = ? ORDER BY role", String.class, id);
        List<Long> domainIds = jdbc.queryForList("SELECT domain_id FROM domain_leads WHERE user_id = ? AND start_date <= CURRENT_DATE AND (end_date IS NULL OR end_date >= CURRENT_DATE) ORDER BY domain_id", Long.class, id);
        return new AccountResponse(id, email, name, program, active, roles, domainIds, setupRequired);
    }

    private void assignDomainRow(long userId, long domainId, long actorId) {
        jdbc.update("INSERT INTO domain_leads (domain_id, user_id, start_date, end_date, assigned_by) VALUES (?, ?, CURRENT_DATE, NULL, ?) ON CONFLICT (domain_id, user_id) DO UPDATE SET start_date = CURRENT_DATE, end_date = NULL, assigned_by = EXCLUDED.assigned_by, created_at = now()",
                domainId, userId, actorId);
    }

    private void insertRoles(long userId, Set<String> roles) {
        for (String role : roles) jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, role);
    }

    private void validateDomains(List<Long> domainIds) {
        for (Long id : domainIds) if (id == null || !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM domains WHERE id = ?)", Boolean.class, id)))
            throw new BadRequestException("A requested domain does not exist");
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) throw new BadRequestException("At least one role is required");
        Set<String> normalized = roles.stream().map(role -> role == null ? "" : role.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!VALID_ROLES.containsAll(normalized)) throw new BadRequestException("One or more roles are invalid");
        return normalized;
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String normalizeProgram(String program) {
        String normalized = program == null ? "TECHNICAL" : program.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("TECHNICAL", "FOUNDATION").contains(normalized)) throw new BadRequestException("Program must be TECHNICAL or FOUNDATION");
        return normalized;
    }
    private boolean emailExists(String email) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM users WHERE lower(email) = ?)", Boolean.class, email)); }
    private boolean isSuperAdmin(CurrentUser actor) { return actor.roles().contains("SUPER_ADMIN"); }
    private void requireMailDelivery() { if (!delivery.isAvailable()) throw new DeliveryUnavailableException(); }
    private void ensureNotLastActiveAdmin() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users u JOIN user_roles r ON r.user_id = u.id WHERE u.active = TRUE AND r.role = 'SUPER_ADMIN'", Integer.class);
        if (count == null || count <= 1) throw new BadRequestException("The last active Super Admin cannot be demoted or deactivated");
    }
    private String hashToken(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 is unavailable", impossible); }
    }
}
