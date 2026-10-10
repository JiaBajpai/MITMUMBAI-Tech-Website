# Render backend deployment

This guide configures the existing Spring Boot service as a Render Web Service. It does not deploy the service or change Supabase. Keep `backend/.env.local` local; Render must receive its own environment variables through the Render Dashboard or a managed secret store.

## Render service settings

- **Service type/runtime:** Web Service, Docker
- **Root directory:** `backend`
- **Dockerfile path:** `./Dockerfile`
- **Docker build context:** `.`
- **Health check path:** `/actuator/health`
- **Production profile:** set `SPRING_PROFILES_ACTIVE=prod` in the Render service environment.

Render does not provide a native Java runtime, so this service uses the Java 25 Docker images in `backend/Dockerfile`. The build stage runs the repository Maven Wrapper:

```sh
./mvnw -B -DskipTests dependency:go-offline
./mvnw -B -DskipTests package
```

The image starts with:

```sh
java -jar /app/app.jar
```

The application listens on `${PORT:8080}` and binds to `0.0.0.0` in production. Render supplies `PORT` (its web-service default is 10000); the application uses 8080 when run locally without `PORT`. Do not set a fixed Render port unless intentionally overriding Render's assigned port.

## Environment variables

Set the following in Render. Secret values belong only in Render's secret environment-variable fields. Do not put them in `render.yaml`, source code, frontend `VITE_*` variables, or logs.

| Variable | Required? | Secret? | Notes |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Yes | No | Set to `prod`. |
| `PORT` | Supplied by Render | No | The application binds to it; do not copy a local port value into the frontend. |
| `DATABASE_URL` | Yes | Sensitive configuration | JDBC PostgreSQL URL. Do not embed username/password in it. |
| `DATABASE_USERNAME` | Yes | No (account identifier) | Supabase database role. |
| `DATABASE_PASSWORD` | Yes | **Yes** | Supabase database password. |
| `DATABASE_SSLMODE` | No (recommended explicitly) | No | Set to `require`; production defaults to this value if omitted. |
| `JWT_SECRET` | Yes | **Yes** | Strong production signing key; never reuse the development fallback. |
| `CORS_ALLOWED_ORIGINS` | Yes | No | Comma-separated exact HTTPS frontend origins, with no wildcard. |
| `GITHUB_CLIENT_ID` | Yes | No | OAuth App client ID; the production profile requires it at startup. |
| `GITHUB_CLIENT_SECRET` | Yes | **Yes** | OAuth App secret; the production profile requires it at startup. |
| `GITHUB_REDIRECT_URI` | Yes | No | Must exactly match the OAuth App callback URL below; required by the production profile. |
| `GITHUB_TOKEN_ENCRYPTION_KEY` | Yes | **Yes** | Stable Base64-encoded 32-byte key used to encrypt stored GitHub tokens. Do not rotate casually; existing ciphertext depends on it. |
| `MAIL_ENABLED` | No | No | Defaults to `false`. Leave false if SMTP is not configured. Disabled mail health does not make aggregate health fail. |

If `MAIL_ENABLED=true`, configure the mail values used by the application:

| Variable | Required when mail is enabled? | Secret? |
| --- | --- | --- |
| `SMTP_HOST` | Yes | No |
| `SMTP_PORT` | Yes | No |
| `SMTP_USERNAME` | If the SMTP provider authenticates | Usually **yes** |
| `SMTP_PASSWORD` | If the SMTP provider authenticates | **Yes** |
| `SMTP_AUTH` | Optional; defaults to `true` | No |
| `SMTP_STARTTLS` | Optional; defaults to `true` | No |
| `MAIL_FROM` | Configure a valid sender address | No |
| `FRONTEND_BASE_URL` | Configure when sending invitation/password-setup links | No |

Other code-supported optional overrides are `JWT_ACCESS_TOKEN_EXPIRATION_MS`, `JWT_REFRESH_TOKEN_EXPIRATION_MS`, `REFRESH_COOKIE_NAME`, `REFRESH_COOKIE_SECURE`, `REFRESH_COOKIE_SAME_SITE`, `GITHUB_SCOPE`, `GITHUB_OAUTH_AUTHORIZE_URL`, `GITHUB_OAUTH_TOKEN_URL`, and `GITHUB_API_URL`. The production refresh cookie is Secure by default. Keep `REFRESH_COOKIE_SECURE=true` behind Render HTTPS.

## Health check

The public Actuator health path is `/actuator/health`. In production, component details are hidden; a healthy response is `{"status":"UP"}`. The aggregate status includes the database health check. The mail health contributor is disabled while `MAIL_ENABLED=false`, so an intentionally unconfigured SMTP service does not make this health check report DOWN. Do not expose additional Actuator endpoints publicly.

## Frontend origin, cookies, and proxy

Set `CORS_ALLOWED_ORIGINS` to the frontend's deployed origin exactly, including `https://` and without a path, for example `https://<frontend-host>`. For multiple approved frontends, use a comma-separated list. The API allows credentialed CORS only for these exact origins, and the browser-origin guard also checks cookie-based login, refresh, and logout requests.

The refresh token is an HttpOnly cookie scoped to `/api/v1/auth`; production sets Secure by default. The default `SameSite=Lax` works for same-site deployments. If the frontend and API are on different sites (for example, unrelated custom domains), set `REFRESH_COOKIE_SAME_SITE=None` and keep `REFRESH_COOKIE_SECURE=true`; the browser client must use credentials. HTTPS terminates at Render's proxy, and Spring's forwarded-header strategy is enabled for the forwarded request scheme/host.

## GitHub OAuth callback

Register this exact callback URL in the GitHub OAuth App and set the same string as `GITHUB_REDIRECT_URI`:

```text
https://<render-backend-host>/api/v1/github/oauth/callback
```

Replace `<render-backend-host>` with the deployed backend's actual Render hostname or configured custom domain. The OAuth App callback, `GITHUB_REDIRECT_URI`, and the public backend host must agree exactly. Do not put the client secret or token encryption key in the frontend.

## Database and data safety

Production uses `classpath:db/migration-production`; the Maven resource bundle excludes V9 and V10. Flyway validation remains enabled, Flyway clean is disabled, and normal application startup does not provision users. Do not change the profile to `demo` or run the historical demo migrations against Supabase. The initial Super Admin already exists; do not run its one-time bootstrap again.

After configuring Render variables, deploy from Render and verify its deployment events and `/actuator/health` check. This repository configuration does not itself deploy the service or prove Render-to-Supabase network connectivity.
