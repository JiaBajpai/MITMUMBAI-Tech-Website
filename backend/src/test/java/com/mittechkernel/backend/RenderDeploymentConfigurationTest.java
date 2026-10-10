package com.mittechkernel.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

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
    }

    @Test
    void productionHealthAndCookieSettingsAreSafeForRender() throws IOException {
        PropertySource<?> common = yaml.load("application", new ClassPathResource("application.yml")).getFirst();
        PropertySource<?> production = yaml.load("application-prod", new ClassPathResource("application-prod.yml")).getFirst();

        assertThat(production.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(production.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(common.getProperty("management.health.mail.enabled")).isEqualTo("${MAIL_ENABLED:false}");
        assertThat(production.getProperty("app.security.refresh-cookie.secure")).isEqualTo("${REFRESH_COOKIE_SECURE:true}");
        assertThat(production.getProperty("app.security.refresh-cookie.same-site")).isEqualTo("${REFRESH_COOKIE_SAME_SITE:Lax}");
    }
}
