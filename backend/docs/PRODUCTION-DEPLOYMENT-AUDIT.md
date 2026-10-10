# Production deployment audit and Supabase setup

## Phase 1 migration-constraint report

This report is based on repository sources only. No database connection or
query was made for this task.

- V9 (`V9__seed_demo_users.sql`) writes to `users`, `user_roles`,
  `user_profiles`, `domain_leads`, and `domain_members`. It inserts six
  example accounts (one each for `SUPER_ADMIN`, `CORE_MEMBER`, `FACULTY`, and
  `STUDENT`, plus two `DOMAIN_LEAD` accounts); six role assignments; profiles
  for the backend lead and student; three lead/domain assignments; and seven
  domain memberships. The lead rows assign Backend and AI/ML/IoT to one lead
  and Frontend to the other. The membership rows refer to those accounts and
  domains. V9 depends on the five domain reference rows from V8 and on the
  `users`/`user_roles`/profile/domain tables created in earlier migrations.
- V10 (`V10__seed_demo_data.sql`) writes to `sessions` (10), `attendance`
  (11), `resources` (9), `tasks` (8), `task_completions` (3), `projects` (6),
  `project_members` (9), `work_items` (13), `activities` (10), `messages`
  (6), `xp_events` (15), `skills` (10), `user_achievements` (2),
  `notifications` (4), and `audit_logs` (3). These are illustrative club
  records, including sample session attendance, example.org resources,
  fabricated project/task activity and GitHub evidence, XP/skill values,
  notifications, and audit entries. V10 resolves users and domains by email
  and name; attendance and completions depend on sessions/tasks; projects
  depend on users/domains and their child rows depend on those projects; skill
  and achievement rows depend on V8 reference records; audit rows refer to
  actors and, where applicable, projects/tasks. It therefore depends on V9
  accounts and V8 reference rows as well as its own preceding inserts.
- V11–V17 contain schema changes only. Inspection found no later migration
  selecting, inserting, or requiring any V9/V10 seeded account or business
  row. V11–V13 add GitHub schema and task/contribution columns; V14 adds an XP
  uniqueness index; V15–V17 add account-management/authentication schema.
- Removing V9/V10 from the production migration location does not cause
  V11–V17 to fail on a fresh schema: their DDL depends on tables/columns from
  V1–V8, not on demo records. V8's domain and achievement definitions remain
  required reference data. Application workflows that need an administrator
  still require explicit account provisioning after schema migration.
- Flyway validates already-applied migration metadata/checksums against the
  configured migration location. If a database records V9 or V10 but uses the
  production location that excludes them, those applied migrations are
  unresolved and validation can fail. If both were applied successfully with
  the repository checksums, an operator may use the full historical location
  to validate that database; Flyway will not rerun successful versions. A
  partial, failed, or checksum-mismatched history requires an operator-led
  forward-only compatibility decision. No baseline, repair, or cleanup is a
  safe automatic workaround. Existing rows cannot be classified or removed
  from names/emails or seed resemblance alone.

This is the required migration constraint: the immutable V9/V10 scripts seed
demo data when selected and pending, while omitting them makes existing
histories that already record them unresolved. The profile-specific generated
production migration location is the safe fresh-install path; compatibility
for already-migrated databases requires inspecting their Flyway history and
choosing the documented location explicitly. Mixed demo and genuine records
remain untouched pending a separately authorized, evidence-based review.

This is an operational audit of repository sources, not a production
approval. No database was queried, so the existing Supabase target, schema,
and Flyway history remain unverified. Do not point the application at it
until its identity and read-only migration history have been reviewed.

## Findings from this repository

- The backend is a Spring Boot application using PostgreSQL, Spring Data JPA,
  Flyway, BCrypt passwords, access JWTs, and rotating refresh-token cookies.
- `application-prod.yml` requires `DATABASE_URL`, `DATABASE_USERNAME`,
  `DATABASE_PASSWORD`, `JWT_SECRET`, exact `CORS_ALLOWED_ORIGINS`, GitHub OAuth
  values, and the GitHub token encryption key. SMTP is separately controlled
  by `MAIL_ENABLED` and the `SMTP_*` variables.
- The local `backend/.env.local` currently contains GitHub variable names only;
  it does not configure `DATABASE_URL`. Its values were not read or printed.
- An earlier audit reported that `localhost:5433` did not respond to
  `pg_isready`; this task did not attempt a database connection, so current
  availability and migration history are unknown. No Supabase database
  connection was made, and use of the existing Supabase project cannot be
  confirmed from repository configuration.
- Production now requires PostgreSQL SSL by default (`DATABASE_SSLMODE=require`),
  disallows Flyway `clean`, and will not automatically baseline an existing
  database. An existing non-empty database without `flyway_schema_history`
  must be reviewed and deliberately baselined by an operator; do not turn
  `baseline-on-migrate` back on as a workaround.
- The default `dev` and `prod` profiles use the generated migration bundle
  that omits V9 and V10. Only the explicit `demo` profile uses the complete
  historical migration directory. Included bundle migrations are copied
  byte-for-byte from their historical sources.
- No student roster was supplied or imported.

## Migration and demo-data review

Migrations are `V1` through `V17`. The schema covers:

`users`, `user_roles`, `refresh_tokens`, `user_profiles`, `domains`,
`domain_leads`, `domain_members`, `sessions`, `attendance`, `resources`,
`tasks`, `task_completions`, `projects`, `project_members`, `work_items`,
`activities`, `messages`, `skills`, `xp_events`, `achievements`,
`user_achievements`, `notifications`, `audit_logs`,
`project_github_repositories`, `github_identities`, `github_credentials`,
`github_contributions`, and `password_setup_tokens`.

The main relationships are: roles, refresh tokens, profile, domains, and
account tokens belong to users; domain leads/members join users to domains;
sessions belong to domains and may reference a lead; attendance joins users to
sessions; resources and tasks belong to domains/sessions; completions join a
task to a user; projects belong to a domain and creator and own membership,
work-item, message, activity, and GitHub-repository records; verified GitHub
contributions link repository/project, optionally a local user, and a verifier;
skills and XP belong to users and domains/source records; achievements join
users through `user_achievements`; notifications belong to users; audit rows
may reference an actor. Foreign-key delete behavior varies by relationship and
must be respected by any approved cleanup.

- V8 inserts reference domains and achievement definitions. These are
  application reference data, not member accounts.
- V9 is explicitly a demo-user seed. It inserts 6 users and role rows, then
  profile rows for 2 users, 3 domain-lead assignments across 2 leads, and
  domain memberships for 7 user/domain pairs. Its source contains a shared demo
  password hash and assigns a seeded `SUPER_ADMIN` role.
- The six V9 accounts are `superadmin@kernel.ac.in` (Aarav Sharma,
  `SUPER_ADMIN`), `core@kernel.ac.in` (Diya Patel, `CORE_MEMBER`),
  `lead.backend@kernel.ac.in` (Prem Kumar, `DOMAIN_LEAD`),
  `lead.frontend@kernel.ac.in` (Sara Khan, `DOMAIN_LEAD`),
  `faculty@kernel.ac.in` (Prof. R. Iyer, `FACULTY`), and
  `student@kernel.ac.in` (Alex Thomas, `STUDENT`). Only the backend lead and
  student receive profiles. The three lead/domain assignments and seven
  domain memberships refer to those same accounts and V8 domains.
- V10 is explicitly a walkthrough/demo seed. It inserts 10 sessions, 11
  attendance rows, 9 resources (several use `example.org` URLs), 8 tasks, 3
  task completions with fabricated GitHub repository URLs/commit SHAs, 6
  projects, 9 project memberships, 13 work items, 10 activities, 6 messages,
  15 XP events, 10 skill rows, 2 earned achievements, 4 notifications, and 3
  audit rows. It does not insert rows into the GitHub identity, encrypted
  credential, project repository, or contribution tables.
- V10's session topics are Club Kickoff, REST APIs with Spring Boot, React
  State Management, PostgreSQL Fundamentals, Intro to Machine Learning,
  Algorithmic Problem Solving, Open Source Contribution Day, Accessible UI
  Patterns, Data Cleaning with Python, and Mock Contest. Resource titles are
  Spring Boot Reference Guide, Building REST APIs Course, PostgreSQL JOINs
  Explained, SQL Practice Platform, React Query Essentials, React Official
  Docs, Kaggle Intro to Machine Learning, LeetCode Patterns, and Redis
  Fundamentals. Task titles are Build a CRUD API, Add JWT security, Design a
  normalized schema, Write five JOIN queries, Implement a data-fetching hook,
  Solve ten graph problems, Train a binary classifier, and Raise a docs pull
  request. Projects are Campus Feed API, ML Study Buddy, CP Judge Lite,
  Portfolio Reviewer, Smart Attendance Bot, and Legacy Site Refresh. Child
  rows join to these V10 rows and the V9 users; exact fields remain in the
  immutable migration source.
- V1–V7 and V11–V17 define schema and application features. Rows in a live
  database may have changed since creation; migration origin alone does not
  prove that a present row is safe to delete.
- No migration was edited and no records were deleted. No database connection
  was made during this task, so live row provenance remains unknown. Do not
  run cleanup against a real database until an authorized operator has
  reviewed Flyway history and exact candidate rows. The production
  initialization path omits these seeds on a new database; existing rows are
  retained pending review.

## Flyway separation and existing database compatibility

The original files in `src/main/resources/db/migration` are immutable history.
Maven packages the same schema/reference migrations under
`db/migration-production`, excluding only V9 and V10. Both
`application-dev.yml` and `application-prod.yml` select that path.
`application-demo.yml` alone selects the complete historical path; its
profile is not grouped with dev. V8's fixed domain and achievement
definitions remain as application reference data. V11–V17 remain in the
demo-free set and are copied without SQL changes. Tests verify these selected
locations and byte equality for every included migration.

Automatic Flyway baselining is disabled for ordinary, demo, and production
configuration. An untracked non-empty schema must be reviewed and handled by
an approved operator procedure; the app will not mark unknown schema history
as managed. Flyway remains the sole schema manager.

For a disposable local fixture database only, explicitly opt into the full
historical seed migrations:

```sh
cd backend
./mvnw -Dspring-boot.run.profiles=demo spring-boot:run
```

Never use the `demo` profile with a shared, genuine-data, staging, or
production database. Both `dev` and `prod` omit V9/V10.

For a genuinely new production database, run the application with the `prod`
profile only after verifying the empty target and reviewing pending schema
migrations. Flyway initializes the schema without demo accounts or V10 sample
business rows. It does not baseline automatically.

For an existing database, inspect `flyway_schema_history` read-only before
startup. If V9/V10 are recorded there, the normal `dev` and `prod` locations
cannot resolve those versions and Flyway validation will fail. Do not use the
`demo` profile to work around it: pending V9/V10 can seed data. Stop and have
the database owner review exact versions, checksums, success states, and
existing records, then approve a forward-only compatibility plan. If neither
version is recorded, the demo-free bundle skips both scripts and applies the
schema/reference migrations it contains. Never infer that existing records
are demo records based on names or emails.

Flyway migration gaps at versions 9 and 10 are intentional in a fresh
production database. This is a profile-specific migration selection, not a
rewrite of old migrations or a claim that demo rows in an existing database
are disposable.

Other pending migrations have independent data-compatibility constraints on
existing databases: V14 creates a unique index over non-null XP source
references, and V16 creates a case-insensitive unique index over user email.
V10's XP rows have null source references, but genuine existing rows may not;
existing accounts may also differ only by email case. Review candidate values
read-only before applying those versions. Do not delete or merge genuine rows
automatically to make either migration pass.

## Explicit initial Super Admin provisioning

The dedicated command supports the existing local development target and a
separately confirmed production target. It is never an application startup
runner. It prompts for the password twice
without terminal echo, uses the configured BCrypt encoder, takes a PostgreSQL
transaction advisory lock, and refuses if any Super Admin exists or if
`MITTechKernel@mitmumbai.com` is already assigned to an account. It creates no
public provisioning endpoint. The command disables Flyway for its own process;
schema migrations must already have been applied through the normal reviewed
deployment.

First apply and verify the production schema, then use a real interactive
terminal with the same production secret-manager environment as the backend.
Set the confirmation to the exact database target in `host:port/database`
format; the command parses `DATABASE_URL` and refuses a mismatch. The
confirmation is an identifier, not a password or secret:

```sh
KERNEL_INITIAL_ADMIN_BOOTSTRAP=true \
KERNEL_INITIAL_ADMIN_PRODUCTION_CONFIRMATION='<host>:<port>/<database>' \
./mvnw -Dspring-boot.run.profiles=prod \
  -Dspring-boot.run.main-class=com.mittechkernel.backend.bootstrap.InitialSuperAdminProvisioningCommand \
  spring-boot:run
```

Enter and confirm the initial password only at the hidden terminal prompt. Do
not put it in shell arguments, environment variables, source, logs, or chat.
The password must be 12–128 characters. On success, the command reports the
account ID; verify by signing in through the deployed frontend. The command
does not migrate the database or expose a setup token. If an administrator
already exists, use the organization-approved recovery path; bootstrap refuses
to create or elevate another account. For production, first apply the schema
through the reviewed production deployment, then run this dedicated command
from a trusted operator workstation/terminal with secrets injected by the
deployment secret manager. Exact remote host, port, and database confirmation
and a hidden interactive password prompt are required. This repository does
not define the organization’s identity recovery procedure; the production
owner must approve it before launch.

For local development, the existing command remains restricted to
`localhost:5433/tech_kernel` and the `dev` profile:

```sh
KERNEL_INITIAL_ADMIN_BOOTSTRAP=true \
./mvnw -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.main-class=com.mittechkernel.backend.bootstrap.InitialSuperAdminProvisioningCommand \
  spring-boot:run
```

The current public routes `/events` and `/projects` show empty showcase states.
The authenticated project, session, task, attendance, resource, and GitHub
screens call backend APIs and use empty/error states. Educational domain
interactions are explicitly labeled simulations. Legacy leaderboard and
recommendation components now redirect to their API-backed/unavailable routes
instead of displaying hardcoded examples. The legacy terminal demo no longer
claims to show verified events or live backend/security status. The public
Domains page contains clearly labeled educational simulations, not live club
data.

No general runtime startup runner, data initializer, Flyway callback, or
`data.sql` seed path was found. Initial Super Admin provisioning is an explicit
operator command; application startup does not create an administrator. No
actual account or business row can be identified from repository sources.
The database was not inspected or modified, so existing real and demo records
are unknown and preserved.

The backend has standard PostgreSQL JDBC configuration and does not configure
a Supabase project or SDK. Production connection variables are
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and
`DATABASE_SSLMODE` (default `require`). Also provide `JWT_SECRET`,
`CORS_ALLOWED_ORIGINS`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`,
`GITHUB_REDIRECT_URI`, and `GITHUB_TOKEN_ENCRYPTION_KEY`; SMTP variables are
needed only when mail is enabled. Frontend configuration uses the public
`VITE_API_BASE_URL`, never database or service-role credentials. Choose the
existing Supabase project's PostgreSQL direct/session-pooler parameters and
TLS mode from that project's settings, then provide them through the backend
deployment secret manager. No credentials were read, and no Supabase
connection or TLS verification was attempted.

No cleanup plan with target-specific row counts can be produced until the
database target and contents have been inspected. Do not delete anything based
on seed-looking names or migration provenance alone. A separate read-only
inventory and approval are required before preparing an exact cleanup script.

## Remaining operational decisions and verification limits

- Before deploying to an existing database, an authorized operator must
  inspect Flyway history read-only and select the compatible migration
  location. Never infer row provenance from names, emails, titles, or
  resemblance to seed rows. In a mixed database, preserve all rows until an
  evidence-based review and separately approved retention plan exist.
- For a new production database, verify the target through the provider's
  control plane, inject production secrets, deploy with the `prod` profile,
  and allow Flyway to apply the demo-free bundle. V8 reference definitions
  remain; V9/V10 business data do not. Then provision the first administrator
  through the explicit command above.
- This repository review did not inspect live database connectivity, state,
  migration history, account existence, or production behavior. Those remain
  unverified. Partial or divergent V9/V10 histories require an
  operator-approved forward-only compatibility plan.
- The complete integration test suite is database-backed and may execute SQL
  against its configured datasource. It was not run because database access
  was prohibited. Run it only against a disposable isolated test database,
  never local genuine data or production.
- Verification for this change: `./mvnw test-compile` succeeded; the selected
  database-independent suite (`ProductionFlywayMigrationBundleTest`,
  `EmptyCollectionApiResponseTest`, `InitialSuperAdminProvisioningCommandTest`,
  `ArchitectureTests`, `DefaultGitHubClientTest`, and
  `GitHubConnectionServiceTest`) passed 30 tests with no failures/errors;
  frontend `npm run build` and `npm run lint` passed; `git diff --check`
  passed; and V1–V10 have no tracked content changes. The full backend suite
  was not run because integration tests can access the configured database.
  No database tests or connectivity checks were run.

## Supabase PostgreSQL configuration

The application uses the PostgreSQL JDBC driver, not Supabase Auth or the
Supabase JavaScript client. Do not put a Supabase service-role key in the
frontend. Configure the backend with the database connection details from the
Supabase dashboard:

- `DATABASE_URL`: JDBC form, for example
  `jdbc:postgresql://<host>:<port>/<database>?sslmode=require`.
- `DATABASE_USERNAME`: the database username supplied for the chosen direct or
  pooler connection mode. Pooler usernames can differ from the direct database
  username.
- `DATABASE_PASSWORD`: the password for that database role.
- `DATABASE_SSLMODE`: defaults to `require`; use the provider's recommended
  certificate-verifying mode if the runtime has the required CA configuration.

Use Supabase's direct connection for a persistent server when network routing
supports it. If the deployment platform is IPv4-only, use the Supabase session
pooler or another mode that the platform supports. Do not put username or
password credentials inside `DATABASE_URL`; the application expects the
separate variables above. Keep all values in the backend host's secret manager.

Before starting the app against any existing Supabase project, use a
read-only database session to verify the project/host and inspect migration
history:

```sql
SELECT installed_rank, version, description, type, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Also inventory existing schemas and table/row counts without selecting
password hashes, token material, or personal data. Compare the live schema and
migration checksums with this repository. If the history table is absent or
versions/checksums differ, stop and resolve the baseline/history plan before
starting the backend. Startup runs Flyway automatically.

The application authenticates requests itself and connects to PostgreSQL using
the configured database role. Supabase RLS does not replace backend
authorization: a privileged/table-owner database role may bypass RLS. If RLS is
enabled, verify its behavior using the exact database role used by Spring Boot
and retain all server-side authorization checks.

## Production environment and launch

Configure these names in the backend runtime's secret/configuration manager;
do not commit them or put them in a `VITE_*` variable:

`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `DATABASE_SSLMODE`,
`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `GITHUB_CLIENT_ID`,
`GITHUB_CLIENT_SECRET`, `GITHUB_REDIRECT_URI`,
`GITHUB_TOKEN_ENCRYPTION_KEY`, and—when invitations/password setup are
enabled—`MAIL_ENABLED`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`,
`SMTP_PASSWORD`, `MAIL_FROM`, and `FRONTEND_BASE_URL`.

The JWT signing secret and GitHub AES key are separate secrets. The GitHub key
must remain stable while encrypted GitHub credentials exist. Use HTTPS callback
and frontend origins in production. CORS accepts exact origins; do not use
wildcards. Set the refresh cookie secure flag to true behind HTTPS.

Build and launch from `backend/` after environment values have been injected
by the host:

```sh
./mvnw clean package
java -jar target/tech-kernel-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Do not launch with the `dev` profile in production. Review migration history,
backup/restore capability, and the pending migrations before the first start.

## Remaining production gaps

- Existing partial or checksum-mismatched V9/V10 histories need an approved
  forward-only compatibility procedure before deployment.
- There is no evidence here that the existing Supabase database matches this
  schema or has any particular Flyway history. No migration was run against
  it.
- The backend has no demonstrated request rate limiter for login/password
  setup. Put a carefully configured limit at the trusted ingress or implement
  a tested backend limit before public launch.
- Email delivery is disabled by default; invitations and password reset need
  working SMTP configuration and a verified frontend origin.
- Configure and test automated database backups and a restore drill before
  production use. Health checks alone do not prove recoverability.
- This audit did not verify a deployed runtime, real GitHub OAuth exchange,
  real SMTP delivery, production CORS, TLS termination, or Supabase RLS policy.
