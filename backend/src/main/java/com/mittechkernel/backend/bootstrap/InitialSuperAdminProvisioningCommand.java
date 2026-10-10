package com.mittechkernel.backend.bootstrap;

import com.mittechkernel.backend.TechKernelApplication;
import com.mittechkernel.backend.modules.admin.service.InitialSuperAdminBootstrapService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.Console;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;

/** Explicit operator command. This class is never run as part of normal application startup. */
public final class InitialSuperAdminProvisioningCommand {
    private static final String OPT_IN_VARIABLE = "KERNEL_INITIAL_ADMIN_BOOTSTRAP";
    private static final String PRODUCTION_CONFIRMATION_VARIABLE = "KERNEL_INITIAL_ADMIN_PRODUCTION_CONFIRMATION";

    private InitialSuperAdminProvisioningCommand() {}

    public static void main(String[] args) {
        if (args.length != 0 || !isExplicitlyEnabled(System.getenv(OPT_IN_VARIABLE))) {
            System.err.println("Refusing to run. Set KERNEL_INITIAL_ADMIN_BOOTSTRAP=true for this command invocation.");
            System.exit(2);
        }

        Console console = System.console();
        if (console == null) {
            System.err.println("A real interactive terminal is required so the password can be entered without echo.");
            System.exit(2);
        }

        System.setProperty("spring.devtools.restart.enabled", "false");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TechKernelApplication.class)
                .web(WebApplicationType.NONE)
                .initializers(applicationContext -> requireSafeBootstrapTarget(
                        applicationContext.getEnvironment(), System.getenv(PRODUCTION_CONFIRMATION_VARIABLE)))
                // Provisioning must never implicitly apply schema/data migrations.
                .run("--spring.flyway.enabled=false")) {
            InitialSuperAdminBootstrapService service = context.getBean(InitialSuperAdminBootstrapService.class);
            service.assertProvisionable();

            String profile = context.getEnvironment().getActiveProfiles()[0];
            console.printf("Provisioning initial Super Admin %s using the explicitly selected %s database.%n",
                    InitialSuperAdminBootstrapService.INITIAL_ADMIN_EMAIL, profile);
            char[] password = console.readPassword("Initial password (12-128 characters): ");
            char[] confirmation = console.readPassword("Confirm initial password: ");
            try {
                if (password == null || confirmation == null) {
                    throw new IllegalArgumentException("Password input was cancelled");
                }
                InitialSuperAdminBootstrapService.validatePassword(password);
                if (!constantTimeEquals(password, confirmation)) {
                    throw new IllegalArgumentException("Password confirmation did not match");
                }
                long id = service.provision(password);
                console.printf("Created active SUPER_ADMIN account id=%d for %s.%n",
                        id, InitialSuperAdminBootstrapService.INITIAL_ADMIN_EMAIL);
                console.printf("Sign in through the existing frontend. No password or setup token was displayed.%n");
            } finally {
                if (password != null) Arrays.fill(password, '\0');
                if (confirmation != null) Arrays.fill(confirmation, '\0');
            }
        } catch (RuntimeException failure) {
            System.err.println(safeFailureMessage(failure));
            System.exit(1);
        }
    }

    static boolean isExplicitlyEnabled(String optInValue) {
        return "true".equals(optInValue);
    }

    static String safeFailureMessage(RuntimeException failure) {
        String message = failure.getMessage();
        if (message != null && (message.startsWith("A SUPER_ADMIN already exists.")
                || message.startsWith("The requested email already belongs to account id=")
                || message.equals("Password must be between 12 and 128 characters")
                || message.equals("Password must not be blank")
                || message.equals("Password confirmation did not match")
                || message.equals("Password input was cancelled"))) {
            return message;
        }
        return "Initial administrator provisioning failed. Internal details and credentials were withheld.";
    }

    static void requireSafeBootstrapTarget(Environment environment, String productionConfirmation) {
        Set<String> profiles = Set.of(environment.getActiveProfiles());
        boolean development = profiles.equals(Set.of("dev"));
        boolean production = profiles.equals(Set.of("prod"));
        if (!development && !production) {
            throw new IllegalStateException("Bootstrap requires exactly one active profile: dev or prod.");
        }

        String configuredUrl = environment.getProperty("spring.datasource.url", "");
        try {
            if (!configuredUrl.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException();
            URI uri = URI.create(configuredUrl.substring("jdbc:".length()));
            String host = uri.getHost();
            String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            int port = uri.getPort();
            if (uri.getUserInfo() != null || host == null || database.isBlank() || port < 1) {
                throw new IllegalArgumentException();
            }
            boolean loopback = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host);
            if (development) {
                if (!loopback || port != 5433 || !"tech_kernel".equals(database)) {
                    throw new IllegalArgumentException();
                }
                return;
            }
            String expectedConfirmation = host + ":" + port + "/" + database;
            if (loopback || !expectedConfirmation.equals(productionConfirmation)) throw new IllegalArgumentException();
        } catch (RuntimeException invalidOrUnsafeUrl) {
            if (development) {
                throw new IllegalStateException("Development bootstrap is restricted to PostgreSQL on localhost:5433/tech_kernel; configured database was refused.");
            }
            throw new IllegalStateException("Production bootstrap requires KERNEL_INITIAL_ADMIN_PRODUCTION_CONFIRMATION to exactly match the configured database host:port/name; the target was refused.");
        }
    }

    private static boolean constantTimeEquals(char[] left, char[] right) {
        if (left == null || right == null) return false;
        ByteBuffer leftBytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(left));
        ByteBuffer rightBytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(right));
        byte[] a = new byte[leftBytes.remaining()];
        byte[] b = new byte[rightBytes.remaining()];
        leftBytes.get(a);
        rightBytes.get(b);
        try {
            return MessageDigest.isEqual(a, b);
        } finally {
            Arrays.fill(a, (byte) 0);
            Arrays.fill(b, (byte) 0);
            if (leftBytes.hasArray()) Arrays.fill(leftBytes.array(), (byte) 0);
            if (rightBytes.hasArray()) Arrays.fill(rightBytes.array(), (byte) 0);
        }
    }
}
