package com.mittechkernel.backend.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionFlywayMigrationBundleTest {
    private static final List<String> PRODUCTION_MIGRATIONS = List.of(
            "V1__auth_identity.sql",
            "V2__domain.sql",
            "V3__session.sql",
            "V4__resource_task.sql",
            "V5__project.sql",
            "V6__skill_gamification.sql",
            "V7__notification_audit.sql",
            "V8__seed_reference.sql",
            "V11__project_github_repositories.sql",
            "V12__github_commit_contributions.sql",
            "V13__github_contribution_verification.sql",
            "V14__xp_activity_idempotency.sql",
            "V15__account_administration_and_password_setup.sql",
            "V16__case_insensitive_user_email_uniqueness.sql",
            "V17__invalidate_access_sessions_on_password_change.sql"
    );

    @Test
    void productionBundleOmitsDemoMigrationsAndPreservesEveryOtherMigrationByteForByte() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        assertThat(classLoader.getResource("db/migration-production/V9__seed_demo_users.sql")).isNull();
        assertThat(classLoader.getResource("db/migration-production/V10__seed_demo_data.sql")).isNull();
        var bundledSqlNames = Arrays.stream(new PathMatchingResourcePatternResolver(classLoader)
                        .getResources("classpath*:db/migration-production/*.sql"))
                .map(resource -> resource.getFilename())
                .collect(Collectors.toSet());
        assertThat(bundledSqlNames).containsExactlyInAnyOrderElementsOf(PRODUCTION_MIGRATIONS);

        for (String migration : PRODUCTION_MIGRATIONS) {
            byte[] historical = classLoader.getResourceAsStream("db/migration/" + migration).readAllBytes();
            byte[] production = classLoader.getResourceAsStream("db/migration-production/" + migration).readAllBytes();
            assertThat(production).as("production copy of %s", migration).containsExactly(historical);
        }
    }

    @Test
    void productionProfileUsesTheDemoFreeMigrationBundle() throws IOException {
        var sources = new YamlPropertySourceLoader().load(
                "application-prod", new ClassPathResource("application-prod.yml"));
        PropertySource<?> source = sources.getFirst();

        assertThat(source.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration-production");
        assertThat(source.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo(false);
        assertThat(source.getProperty("spring.flyway.clean-disabled")).isEqualTo(true);
    }

    @Test
    void devProfileSelectsDemoFreeBundleForFreshDatabases() throws IOException {
        var common = new YamlPropertySourceLoader().load(
                "application", new ClassPathResource("application.yml")).getFirst();
        var dev = new YamlPropertySourceLoader().load(
                "application-dev", new ClassPathResource("application-dev.yml")).getFirst();

        assertThat(dev.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration-production");
        assertThat(common.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo(false);
        assertThat(getClass().getClassLoader().getResource("db/migration-production/V9__seed_demo_users.sql")).isNull();
        assertThat(getClass().getClassLoader().getResource("db/migration-production/V10__seed_demo_data.sql")).isNull();
    }

    @Test
    void demoProfileUsesTheSameHistoricalChainExplicitly() throws IOException {
        var source = new YamlPropertySourceLoader().load(
                "application-demo", new ClassPathResource("application-demo.yml")).getFirst();
        var common = new YamlPropertySourceLoader().load(
                "application", new ClassPathResource("application.yml")).getFirst();

        assertThat(source.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
        assertThat(common.getProperty("spring.profiles.group.demo[0]")).isNull();
        assertThat(source.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo(false);
    }

    @Test
    void previouslySeededDatabaseCanResolveV9AndV10OnlyThroughExplicitDemoLocation() throws IOException {
        var dev = new YamlPropertySourceLoader().load(
                "application-dev", new ClassPathResource("application-dev.yml")).getFirst();
        var prod = new YamlPropertySourceLoader().load(
                "application-prod", new ClassPathResource("application-prod.yml")).getFirst();
        var demo = new YamlPropertySourceLoader().load(
                "application-demo", new ClassPathResource("application-demo.yml")).getFirst();

        assertThat(dev.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration-production");
        assertThat(prod.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration-production");
        assertThat(demo.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
        for (String migration : List.of("V9__seed_demo_users.sql", "V10__seed_demo_data.sql")) {
            assertThat(getClass().getClassLoader().getResource("db/migration/" + migration)).isNotNull();
            assertThat(getClass().getClassLoader().getResource("db/migration-production/" + migration)).isNull();
        }
    }
}
