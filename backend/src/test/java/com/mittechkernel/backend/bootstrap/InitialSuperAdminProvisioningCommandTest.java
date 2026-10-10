package com.mittechkernel.backend.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitialSuperAdminProvisioningCommandTest {
    @Test
    void provisioningRequiresAnExactExplicitOptIn() {
        assertThat(InitialSuperAdminProvisioningCommand.isExplicitlyEnabled("true")).isTrue();
        assertThat(InitialSuperAdminProvisioningCommand.isExplicitlyEnabled(null)).isFalse();
        assertThat(InitialSuperAdminProvisioningCommand.isExplicitlyEnabled("false")).isFalse();
        assertThat(InitialSuperAdminProvisioningCommand.isExplicitlyEnabled("TRUE")).isFalse();
    }

    @Test
    void unexpectedFailureDetailsCannotLeakCredentialsToTheTerminal() {
        String sentinelPassword = "never-display-this-password";

        assertThat(InitialSuperAdminProvisioningCommand.safeFailureMessage(
                new IllegalStateException("database error involving " + sentinelPassword)))
                .doesNotContain(sentinelPassword)
                .isEqualTo("Initial administrator provisioning failed. Internal details and credentials were withheld.");
    }

    @Test
    void permitsOnlyTheConfiguredLocalDevelopmentDatabase() {
        MockEnvironment environment = environment("dev", "jdbc:postgresql://localhost:5433/tech_kernel");

        assertThatCode(() -> InitialSuperAdminProvisioningCommand.requireSafeBootstrapTarget(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesUnsafeDevelopmentTargetsAndRequiresExactProductionConfirmation() {
        assertThatThrownBy(() -> InitialSuperAdminProvisioningCommand.requireSafeBootstrapTarget(
                environment("prod", "jdbc:postgresql://localhost:5433/tech_kernel"), null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("PRODUCTION_CONFIRMATION");

        for (String url : new String[]{
                "jdbc:postgresql://db.example.com:5433/tech_kernel",
                "jdbc:postgresql://localhost:5432/tech_kernel",
                "jdbc:postgresql://localhost:5433/production"
        }) {
            assertThatThrownBy(() -> InitialSuperAdminProvisioningCommand.requireSafeBootstrapTarget(
                    environment("dev", url), null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("localhost:5433/tech_kernel");
        }
    }

    @Test
    void permitsProductionOnlyWhenConfirmationMatchesTheExactRemoteDatabase() {
        MockEnvironment environment = environment("prod", "jdbc:postgresql://db.example.com:6543/kernel?sslmode=require");

        assertThatCode(() -> InitialSuperAdminProvisioningCommand.requireSafeBootstrapTarget(
                environment, "db.example.com:6543/kernel"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> InitialSuperAdminProvisioningCommand.requireSafeBootstrapTarget(
                environment, "db.example.com:6543/other"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("configured database host:port/name");
    }

    @Test
    void refusesAmbiguousProfilesAndCredentialBearingJdbcUrls() {
        assertThatThrownBy(() -> InitialSuperAdminProvisioningCommand.requireSafeBootstrapTarget(
                environment("dev,prod", "jdbc:postgresql://localhost:5433/tech_kernel"), null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("exactly one active profile");

        assertThatThrownBy(() -> InitialSuperAdminProvisioningCommand.requireSafeBootstrapTarget(
                environment("prod", "jdbc:postgresql://user:secret@db.example.com:6543/kernel"),
                "db.example.com:6543/kernel"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("configured database host:port/name");
    }

    private MockEnvironment environment(String profile, String databaseUrl) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile.split(","));
        environment.setProperty("spring.datasource.url", databaseUrl);
        return environment;
    }
}
