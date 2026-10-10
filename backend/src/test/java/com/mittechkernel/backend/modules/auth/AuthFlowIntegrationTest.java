package com.mittechkernel.backend.modules.auth;

import com.mittechkernel.backend.modules.auth.dto.LoginRequest;
import tools.jackson.databind.ObjectMapper;
import com.mittechkernel.backend.modules.auth.service.AuthService;
import com.mittechkernel.backend.modules.auth.service.JwtService;
import com.mittechkernel.backend.common.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class AuthFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void passwordVerificationWorks() {
        String hash = "$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6";
        assertThat(passwordEncoder.matches("Kernel@123", hash)).isTrue();
    }

    @Test
    void loginSuccessReturnsTokensForDemoUser() throws Exception {
        LoginRequest request = new LoginRequest("student@kernel.ac.in", "Kernel@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .header("Origin", "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andExpect(jsonPath("$.data.user.email").value("student@kernel.ac.in"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void localBrowserOriginsCanPreflightAndReadLoginResponses() throws Exception {
        for (String origin : new String[] {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:4173", "http://127.0.0.1:4173"}) {
            mockMvc.perform(options("/api/v1/auth/login")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .header("Origin", origin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("student@kernel.ac.in", "wrongpass"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }
    }

    @Test
    void corsRejectsUnconfiguredLocalPortsAndExternalOrigins() throws Exception {
        for (String origin : new String[] {"http://localhost:4174", "http://127.0.0.1:4174", "http://localhost:3000", "https://untrusted.example"}) {
            mockMvc.perform(options("/api/v1/auth/login")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "content-type"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void actuatorLivenessAndReadinessArePublicAndReturnOnlyHealthStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void loginFailureReturnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("student@kernel.ac.in", "wrongpass");

        mockMvc.perform(post("/api/v1/auth/login")
                .header("Origin", "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtValidationAndProtectedEndpointWithoutAuthentication() throws Exception {
        String token = jwtService.generateAccessToken(1L, "student@kernel.ac.in");
        assertThat(jwtService.isTokenValid(token)).isTrue();

        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenSuccessRotatesToken() throws Exception {
        String refreshToken = authService.login("student@kernel.ac.in", "Kernel@123").refreshToken();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Origin", "http://localhost:5173")
                .cookie(new Cookie("kernel_refresh", refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Lax")));
    }

    @Test
    void refreshRejectsUntrustedBrowserOriginWithoutConsumingCookie() throws Exception {
        String refreshToken = authService.login("student@kernel.ac.in", "Kernel@123").refreshToken();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Origin", "https://evil.example")
                .cookie(new Cookie("kernel_refresh", refreshToken)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Origin", "http://localhost:5173")
                .cookie(new Cookie("kernel_refresh", refreshToken)))
            .andExpect(status().isOk());
    }

    @Test
    void revokedRefreshTokenFails() throws Exception {
        String refreshToken = authService.login("student@kernel.ac.in", "Kernel@123").refreshToken();
        authService.logout(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Origin", "http://localhost:5173")
                .cookie(new Cookie("kernel_refresh", refreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String refreshToken = authService.login("student@kernel.ac.in", "Kernel@123").refreshToken();

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Origin", "http://localhost:5173")
                .cookie(new Cookie("kernel_refresh", refreshToken)))
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Origin", "http://localhost:5173")
                .cookie(new Cookie("kernel_refresh", refreshToken)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void inactiveAccountCannotLoginRefreshOrUseAnExistingAccessToken() throws Exception {
        var tokens = authService.login("student@kernel.ac.in", "Kernel@123");
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class,
                "student@kernel.ac.in");
        jdbcTemplate.update("UPDATE users SET active = FALSE WHERE id = ?", userId);

        try {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("Origin", "http://localhost:5173")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("student@kernel.ac.in", "Kernel@123"))))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .header("Origin", "http://localhost:5173")
                            .cookie(new Cookie("kernel_refresh", tokens.refreshToken())))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + tokens.accessToken()))
                    .andExpect(status().isUnauthorized());
        } finally {
            jdbcTemplate.update("UPDATE users SET active = TRUE WHERE id = ?", userId);
        }
    }

    @Test
    void removingARoleTakesEffectForAnAlreadyIssuedAccessToken() throws Exception {
        var tokens = authService.login("core@kernel.ac.in", "Kernel@123");
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class,
                "core@kernel.ac.in");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ? AND role = 'CORE_MEMBER'", userId);

        try {
            mockMvc.perform(get("/api/v1/test/core-member")
                            .header("Authorization", "Bearer " + tokens.accessToken()))
                    .andExpect(status().isForbidden());
        } finally {
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'CORE_MEMBER') ON CONFLICT DO NOTHING", userId);
        }
    }

    @Test
    void concurrentRefreshAttemptsConsumeTheTokenOnlyOnce() throws Exception {
        String refreshToken = authService.login("student@kernel.ac.in", "Kernel@123").refreshToken();
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> refreshSuccessfully(refreshToken, start));
            var second = executor.submit(() -> refreshSuccessfully(refreshToken, start));
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isNotEqualTo(second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean refreshSuccessfully(String token, CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            authService.refresh(token);
            return true;
        } catch (InvalidTokenException ex) {
            return false;
        }
    }
}
