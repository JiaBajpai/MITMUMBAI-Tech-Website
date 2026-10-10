package com.mittechkernel.backend.modules.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class InitialSuperAdminBootstrapServiceTest {
    private static final String TEST_PASSWORD = "BootstrapTestOnly#2026";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditLogWriter auditLogWriter;
    @Autowired private InitialSuperAdminBootstrapService productionBootstrapService;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void firstProvisioningCreatesHashedActiveSuperAdminThatCanUseExistingLoginEndpoint() throws Exception {
        jdbc.update("DELETE FROM user_roles WHERE role = 'SUPER_ADMIN'");
        String testEmail = "bootstrap-test-" + UUID.randomUUID() + "@example.test";
        InitialSuperAdminBootstrapService service = serviceFor(testEmail);

        long id = service.provision(TEST_PASSWORD.toCharArray());

        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, id)).isEqualTo(testEmail);
        assertThat(jdbc.queryForObject("SELECT active FROM users WHERE id = ?", Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject("SELECT password_setup_required FROM users WHERE id = ?", Boolean.class, id)).isFalse();
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, id);
        assertThat(hash).startsWith("$2").isNotEqualTo(TEST_PASSWORD);
        assertThat(passwordEncoder.matches(TEST_PASSWORD, hash)).isTrue();
        assertThat(jdbc.queryForList("SELECT role FROM user_roles WHERE user_id = ?", String.class, id))
                .containsExactly("SUPER_ADMIN");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("email", testEmail, "password", TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(testEmail))
                .andExpect(jsonPath("$.data.user.roles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void existingSuperAdminRefusesProvisioningWithoutChangingAccounts() {
        Integer adminsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE role = 'SUPER_ADMIN'", Integer.class);
        Integer usersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

        assertThatThrownBy(() -> productionBootstrapService.provision(TEST_PASSWORD.toCharArray()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role = 'SUPER_ADMIN'", Integer.class))
                .isEqualTo(adminsBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(usersBefore);
    }

    @Test
    void existingRequestedEmailIsReportedWithItsStateAndIsNotModified() {
        jdbc.update("DELETE FROM user_roles WHERE role = 'SUPER_ADMIN'");
        String testEmail = "bootstrap-existing-" + UUID.randomUUID() + "@example.test";
        Long existingId = jdbc.queryForObject("""
                INSERT INTO users (email, name, password_hash, program, active)
                VALUES (?, 'Existing student', ?, 'TECHNICAL', TRUE) RETURNING id
                """, Long.class, testEmail, passwordEncoder.encode("ExistingTestPassword#2026"));
        jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'STUDENT')", existingId);
        String hashBefore = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, existingId);

        assertThatThrownBy(() -> serviceFor(testEmail).provision(TEST_PASSWORD.toCharArray()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active=true")
                .hasMessageContaining("STUDENT");

        assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, existingId))
                .isEqualTo(hashBefore);
        assertThat(jdbc.queryForList("SELECT role FROM user_roles WHERE user_id = ?", String.class, existingId))
                .containsExactly("STUDENT");
    }

    @Test
    void invalidEmailAndPasswordAreRejectedBeforeAnyAccountIsCreated() {
        assertThatThrownBy(() -> serviceFor("invalid-email").provision(TEST_PASSWORD.toCharArray()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("email is invalid");
        assertThatThrownBy(() -> serviceFor("bootstrap-invalid-" + UUID.randomUUID() + "@example.test")
                .provision("short".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("12 and 128");
        assertThatThrownBy(() -> InitialSuperAdminBootstrapService.validatePassword("             ".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> InitialSuperAdminBootstrapService.validatePassword(new char[129]))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("12 and 128");
    }

    @Test
    void concurrentAttemptsAgainstExistingAdminAreBothRefusedWithoutPartialRows() throws Exception {
        int adminsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role = 'SUPER_ADMIN'", Integer.class);
        int usersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> invokeConcurrently(ready, start));
            var second = executor.submit(() -> invokeConcurrently(ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertRefusal(first.get(15, TimeUnit.SECONDS));
            assertRefusal(second.get(15, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles WHERE role = 'SUPER_ADMIN'", Integer.class))
                .isEqualTo(adminsBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(usersBefore);
    }

    @Test
    void normalApplicationContextDoesNotProvisionTheInitialAddress() {
        assertThat(InitialSuperAdminBootstrapService.INITIAL_ADMIN_EMAIL).isEqualTo("MITTechKernel@mitmumbai.com");
        assertThat(applicationContext.getBeansOfType(org.springframework.boot.ApplicationRunner.class).values())
                .noneMatch(runner -> runner.getClass().getName().contains("InitialSuperAdminProvisioning"));
        assertThat(applicationContext.getBeansOfType(org.springframework.boot.CommandLineRunner.class).values())
                .noneMatch(runner -> runner.getClass().getName().contains("InitialSuperAdminProvisioning"));
    }

    private String invokeConcurrently(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
            productionBootstrapService.provision(TEST_PASSWORD.toCharArray());
            return "created";
        } catch (IllegalStateException refusal) {
            return refusal.getMessage();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("concurrent test interrupted", interrupted);
        }
    }

    private InitialSuperAdminBootstrapService serviceFor(String email) {
        return new InitialSuperAdminBootstrapService(jdbc, passwordEncoder, auditLogWriter, email);
    }

    private void assertRefusal(String message) {
        assertThat(message).containsAnyOf("A SUPER_ADMIN already exists", "requested email already belongs");
    }
}
