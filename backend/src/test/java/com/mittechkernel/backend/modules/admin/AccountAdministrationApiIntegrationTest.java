package com.mittechkernel.backend.modules.admin;

import com.mittechkernel.backend.modules.admin.service.PasswordSetupDelivery;
import com.mittechkernel.backend.modules.auth.service.AuthService;
import com.mittechkernel.backend.modules.auth.service.JwtService;
import com.mittechkernel.backend.modules.admin.dto.CreateAccountRequest;
import com.mittechkernel.backend.modules.admin.dto.UpdateRolesRequest;
import com.mittechkernel.backend.modules.admin.dto.UpdateAccountRequest;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(AccountAdministrationApiIntegrationTest.DeliveryTestConfig.class)
@Transactional
class AccountAdministrationApiIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private CapturingDelivery delivery;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        delivery.latestLink.set(null);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void coreMemberCanCreateStudentsButCannotGrantPrivilegedRoles() throws Exception {
        String core = token("core@kernel.ac.in");
        long before = userCount();
        mockMvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer " + core)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest("new.student@kernel.ac.in", "New Student", "FOUNDATION", Set.of("STUDENT"), Set.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        assertThat(userCount()).isEqualTo(before + 1);
        assertThat(delivery.latestLink.get()).contains("/set-password?token=");

        mockMvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer " + core)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest("forged.admin@kernel.ac.in", "Forged", "TECHNICAL", Set.of("SUPER_ADMIN"), Set.of()))))
                .andExpect(status().isForbidden());
        assertThat(userCount()).isEqualTo(before + 1);
    }

    @Test
    void emailIsUniqueAndSystemIdsAreGeneratedUniquely() throws Exception {
        String admin = token("superadmin@kernel.ac.in");
        var request = new CreateAccountRequest("unique.student@kernel.ac.in", "Unique Student", "TECHNICAL", Set.of("STUDENT"), Set.of());
        long firstId = create(admin, request);
        long secondId = create(admin, new CreateAccountRequest("unique2.student@kernel.ac.in", "Unique Student 2", "TECHNICAL", Set.of("STUDENT"), Set.of()));
        assertThat(firstId).isNotEqualTo(secondId);
        mockMvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest("UNIQUE.STUDENT@KERNEL.AC.IN", "Duplicate", "TECHNICAL", Set.of("STUDENT"), Set.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordinaryRolesCannotManageAccounts() throws Exception {
        for (String email : List.of("student@kernel.ac.in", "faculty@kernel.ac.in", "lead.backend@kernel.ac.in")) {
            mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token(email)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void coreMemberCannotChangeRolesOrManageNonStudentAccounts() throws Exception {
        String core = token("core@kernel.ac.in");
        long studentId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'student@kernel.ac.in'", Long.class);
        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", studentId).header("Authorization", "Bearer " + core)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new UpdateRolesRequest(Set.of("SUPER_ADMIN")))))
                .andExpect(status().isForbidden());
        long coreId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'core@kernel.ac.in'", Long.class);
        mockMvc.perform(patch("/api/v1/admin/users/{id}", coreId).header("Authorization", "Bearer " + core)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new UpdateAccountRequest(null, "Self-promoted", null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlySuperAdminCanAssignDomainLeadAndRoleIsMaintained() throws Exception {
        String superAdmin = token("superadmin@kernel.ac.in");
        String core = token("core@kernel.ac.in");
        long studentId = create(superAdmin, new CreateAccountRequest("future.lead@kernel.ac.in", "Future Lead", "TECHNICAL", Set.of("STUDENT"), Set.of()));
        long domainId = jdbc.queryForObject("SELECT id FROM domains ORDER BY id LIMIT 1", Long.class);
        mockMvc.perform(put("/api/v1/admin/users/{id}/domains/{domainId}", studentId, domainId).header("Authorization", "Bearer " + core))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/users/{id}/domains/{domainId}", studentId, domainId).header("Authorization", "Bearer " + superAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles").value(org.hamcrest.Matchers.hasItem("DOMAIN_LEAD")))
                .andExpect(jsonPath("$.data.domainIds").value(org.hamcrest.Matchers.hasItem((int) domainId)));
        mockMvc.perform(delete("/api/v1/admin/users/{id}/domains/{domainId}", studentId, domainId).header("Authorization", "Bearer " + superAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("DOMAIN_LEAD"))));
    }

    @Test
    void passwordSetupLinkIsDeliveredOncePasswordIsHashedAndTokenCannotBeReused() throws Exception {
        String admin = token("superadmin@kernel.ac.in");
        long id = create(admin, new CreateAccountRequest("setup.student@kernel.ac.in", "Setup Student", "TECHNICAL", Set.of("STUDENT"), Set.of()));
        String setupToken = getTokenFromMail();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM password_setup_tokens WHERE user_id = ?", String.class, id)).isNotEqualTo(setupToken);
        assertThat(jdbc.queryForObject("SELECT meta::text FROM audit_logs WHERE action = 'ACCOUNT_CREATE' AND resource_id = ? ORDER BY id DESC LIMIT 1", String.class, id)).doesNotContain(setupToken);

        mockMvc.perform(post("/api/v1/auth/password/setup").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + setupToken + "\",\"password\":\"SafeNewPassword#2026\"}"))
                .andExpect(status().isOk());
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, id);
        assertThat(passwordEncoder.matches("SafeNewPassword#2026", hash)).isTrue();
        assertThat(passwordEncoder.matches(setupToken, hash)).isFalse();
        assertThat(jdbc.queryForObject("SELECT password_setup_required FROM users WHERE id = ?", Boolean.class, id)).isFalse();
        mockMvc.perform(post("/api/v1/auth/password/setup").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + setupToken + "\",\"password\":\"AnotherSafePassword#2026\"}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action = 'PASSWORD_SETUP_COMPLETED' AND resource_id = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void passwordResetRevokesRefreshTokensAndExpiredTokenIsRejected() throws Exception {
        String admin = token("superadmin@kernel.ac.in");
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'student@kernel.ac.in'", Long.class);
        var oldSession = authService.login("student@kernel.ac.in", "Kernel@123");
        Integer refreshCountBeforeExpiredAttempt = jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, userId);
        mockMvc.perform(post("/api/v1/admin/users/{id}/password-reset", userId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        String resetToken = getTokenFromMail();
        jdbc.update("UPDATE password_setup_tokens SET expires_at = now() - interval '1 second' WHERE user_id = ?", userId);
        mockMvc.perform(post("/api/v1/auth/password/setup").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + resetToken + "\",\"password\":\"ExpiredLinkPassword#2026\"}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, userId)).isEqualTo(refreshCountBeforeExpiredAttempt);
        mockMvc.perform(post("/api/v1/admin/users/{id}/password-reset", userId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        String currentToken = getTokenFromMail();
        mockMvc.perform(post("/api/v1/auth/password/setup").header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + currentToken + "\",\"password\":\"ResetToSafePassword#2026\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, userId)).isZero();
        entityManager.clear(); // Account writes use JDBC; clear the login's cached JPA entity before simulating a new request.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldSession.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivationRevokesRefreshTokensAndAuditLogsCannotBeDeleted() throws Exception {
        String admin = token("superadmin@kernel.ac.in");
        long studentId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'student@kernel.ac.in'", Long.class);
        authService.login("student@kernel.ac.in", "Kernel@123");
        mockMvc.perform(patch("/api/v1/admin/users/{id}", studentId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, studentId)).isZero();
        Integer auditCount = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action = 'ACCOUNT_UPDATE' AND resource_id = ?", Integer.class, studentId);
        assertThat(auditCount).isPositive();
    }

    private long create(String token, CreateAccountRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }
    private String token(String email) { return authService.login(email, "Kernel@123").accessToken(); }
    private long userCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class); }
    private String getTokenFromMail() { return URI.create(delivery.latestLink.get()).getQuery().substring("token=".length()); }

    @TestConfiguration
    static class DeliveryTestConfig {
        @Bean @Primary CapturingDelivery testPasswordSetupDelivery() { return new CapturingDelivery(); }
    }

    static class CapturingDelivery implements PasswordSetupDelivery {
        private final AtomicReference<String> latestLink = new AtomicReference<>();
        @Override public boolean isAvailable() { return true; }
        @Override public void send(String email, String name, String link) { latestLink.set(link); }
    }
}
