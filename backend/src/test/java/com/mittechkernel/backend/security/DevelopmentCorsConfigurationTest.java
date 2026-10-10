package com.mittechkernel.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevelopmentCorsConfigurationTest {
    private static final List<String> VITE_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:4173",
            "http://127.0.0.1:4173");

    @Test
    void configuredViteOriginsCanPreflightCredentialedRequestsAndUnknownOriginsRemainBlocked() throws IOException {
        List<String> origins = developmentOriginsFromConfiguration();
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(origins);
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"));

        assertThat(cors.getAllowedOrigins()).containsExactlyElementsOf(VITE_ORIGINS);
        assertThat(cors.getAllowedMethods()).contains("OPTIONS", "POST", "DELETE");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type");
        assertThat(cors.getAllowCredentials()).isTrue();
        for (String origin : VITE_ORIGINS) {
            assertThat(cors.checkOrigin(origin)).isEqualTo(origin);
        }
        assertThat(cors.checkOrigin("http://127.0.0.1:4174")).isNull();
        assertThat(cors.checkOrigin("https://untrusted.example")).isNull();
    }

    @Test
    void browserOriginGuardUsesTheSameDevelopmentAllowlist() throws IOException {
        BrowserOriginGuard guard = new BrowserOriginGuard(developmentOriginsFromConfiguration());

        MockHttpServletRequest allowed = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        allowed.addHeader("Origin", "http://127.0.0.1:4173");
        assertThatCode(() -> guard.requireAllowedOrigin(allowed)).doesNotThrowAnyException();

        MockHttpServletRequest blocked = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        blocked.addHeader("Origin", "http://localhost:3000");
        assertThatThrownBy(() -> guard.requireAllowedOrigin(blocked))
                .isInstanceOf(com.mittechkernel.backend.common.exception.ForbiddenException.class);
    }

    @Test
    void productionCorsRemainsAnExplicitEnvironmentSuppliedAllowlist() throws IOException {
        var source = new YamlPropertySourceLoader().load(
                "application-prod", new ClassPathResource("application-prod.yml")).getFirst();

        assertThat(source.getProperty("app.cors.allowed-origins")).isEqualTo("${CORS_ALLOWED_ORIGINS}");
        assertThat(source.getProperty("app.cors.allowed-origins").toString()).doesNotContain("*");
    }

    private List<String> developmentOriginsFromConfiguration() throws IOException {
        var source = new YamlPropertySourceLoader().load(
                "application-dev", new ClassPathResource("application-dev.yml")).getFirst();
        String configured = source.getProperty("app.cors.allowed-origins").toString();
        String prefix = "${CORS_ALLOWED_ORIGINS:";
        assertThat(configured).startsWith(prefix).endsWith("}");
        String defaults = configured.substring(prefix.length(), configured.length() - 1);
        return Arrays.asList(defaults.split(","));
    }
}
