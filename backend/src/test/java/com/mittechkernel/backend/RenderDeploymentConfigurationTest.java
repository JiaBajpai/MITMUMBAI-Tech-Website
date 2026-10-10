package com.mittechkernel.backend;

import com.mittechkernel.backend.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RenderDeploymentConfigurationTest {
    private final YamlPropertySourceLoader yaml = new YamlPropertySourceLoader();

    @Test
    void productionUsesRenderPortAndBindsThroughTheProxy() throws IOException {
        PropertySource<?> common = yaml.load("application", new ClassPathResource("application.yml")).getFirst();
        PropertySource<?> production = yaml.load("application-prod", new ClassPathResource("application-prod.yml")).getFirst();

        assertThat(common.getProperty("spring.profiles.default")).isEqualTo("dev");
        assertThat(common.getProperty("spring.profiles.active")).isNull();
        assertThat(common.getProperty("server.port")).isEqualTo("${PORT:8080}");
        assertThat(production.getProperty("server.address")).isEqualTo("0.0.0.0");
        assertThat(production.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
        assertThat(production.getProperty("server.servlet.session.cookie.http-only")).isEqualTo(true);
        assertThat(production.getProperty("server.servlet.session.cookie.secure")).isEqualTo(true);
        assertThat(production.getProperty("server.servlet.session.cookie.path")).isEqualTo("/");
        assertThat(production.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("None");
    }

    @Test
    void productionHealthAndCookieSettingsAreSafeForRender() throws IOException {
        PropertySource<?> common = yaml.load("application", new ClassPathResource("application.yml")).getFirst();
        PropertySource<?> production = yaml.load("application-prod", new ClassPathResource("application-prod.yml")).getFirst();

        assertThat(production.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(production.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(common.getProperty("management.health.mail.enabled")).isEqualTo("${MAIL_ENABLED:false}");
        assertThat(production.getProperty("app.security.refresh-cookie.secure")).isEqualTo("${REFRESH_COOKIE_SECURE:true}");
        assertThat(production.getProperty("app.security.refresh-cookie.same-site")).isEqualTo("${REFRESH_COOKIE_SAME_SITE:None}");
    }

    @Test
    void configuredProductionFrontendOriginCanUseCredentialedOAuthInitiation() {
        CorsConfiguration cors = new SecurityConfig()
                .corsConfigurationSource(List.of("https://mit-tech-kernel.vercel.app"))
                .getCorsConfiguration(new MockHttpServletRequest("POST", "/api/v1/me/github/connect"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.checkOrigin("https://mit-tech-kernel.vercel.app"))
                .isEqualTo("https://mit-tech-kernel.vercel.app");
        assertThat(cors.checkOrigin("https://untrusted.example")).isNull();
    }

    @Test
    void prodProfileLoadsTheServletSessionCookieSettings() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("server.servlet.session.cookie.same-site"))
                            .isEqualTo("None");
                    assertThat(context.getEnvironment().getProperty("server.servlet.session.cookie.secure", Boolean.class))
                            .isTrue();
                    assertThat(context.getEnvironment().getProperty("server.servlet.session.cookie.http-only", Boolean.class))
                            .isTrue();
                    assertThat(context.getEnvironment().getProperty("server.servlet.session.cookie.path"))
                            .isEqualTo("/");
                });
    }
}
