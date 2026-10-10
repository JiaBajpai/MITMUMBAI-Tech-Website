# MIT TECH KERNEL Backend

Backend service for the MIT TECH KERNEL technical club platform. Provides REST APIs for authentication, user/member management, technical domains, sessions, attendance, resources, tasks, projects, skills, gamification (XP/achievements), leaderboards, Foundation Program, notifications, analytics, and audit logging.

**Current Development Status**: Foundation/infrastructure complete. Business features are planned but NOT yet implemented. This repository contains the architectural foundation, module structure, configuration, and documentation only.

---

## MVP Feature Scope

The following capabilities are planned for the MVP. They are **not implemented** yet.

- **Authentication & RBAC** — JWT access/refresh tokens, password hashing, role-based access control
- **User & Member Management** — Profiles, skills, domain associations, search/filtering, pagination
- **Technical Domains** — Five fixed domains (General, Frontend, Backend, AI/ML/IoT, Competitive Programming) with domain leads and domain-scoped management
- **Sessions** — Create/manage sessions, scheduling, domain association, participation, history
- **Attendance** — Mark attendance, session-wise attendance, history, analytics
- **Resources** — Curated resource library organized by domain, topic, difficulty, type (documentation, course, tutorial, video, article, practice platform, GitHub repo); optional session association
- **Post-session Tasks** — Create, assign, track status, track completion
- **GitHub Evidence Verification** — Repository URL/branch/commit SHA evidence, format validation, Domain Lead verification (no live GitHub API integration)
- **Projects** — Full lifecycle: Proposal → Approval → Join → Work → Evidence/Activity → Completion
- **Project Discussions** — Threaded discussions with REST polling (no WebSockets)
- **Skills & Skill Scoring** — Track member skills, skill-related activity, scoring, progression
- **Personalized Recommendations** — Projects, tasks, learning opportunities with explainable "why" (deterministic/weighted scoring; no personalized resource recommendations)
- **XP & Achievements** — XP system from activities, five hardcoded MVP achievements
- **Technical Leaderboards** — Overall and domain-specific leaderboards, XP/scoring-based ranking
- **Foundation Program** — Separate program/scope (NOT an RBAC role) with PARTICIPANT/MENTOR/COORDINATOR internal roles, separate Foundation leaderboard
- **Notifications** — User, activity, project, task, and system notifications
- **Analytics** — Attendance, participation, task completion, project activity, member progress, domain activity, leaderboard analytics
- **Audit Logs** — Append-only tracking of important system actions (actor, action, timestamp, affected resource, metadata)
- **Search, Filtering & Pagination** — Applied to members, projects, sessions, resources, leaderboards, and other list-based resources

### Important MVP Clarifications

| Feature | Status |
|---------|--------|
| Personalized resource recommendations | ❌ NOT in MVP |
| Live GitHub API integration | ❌ NOT in MVP |
| WebSockets for real-time | ❌ NOT in MVP (REST polling only) |
| Business feature implementations | ❌ NOT implemented yet |

---

## Technology Stack

| Technology | Version | Source |
|------------|---------|--------|
| Java | 25 (LTS) | `pom.xml:30` |
| Spring Boot | 4.1.1 | `pom.xml:8` |
| Maven | 3.8+ (wrapper included) | `.mvn/wrapper/maven-wrapper.properties` |
| PostgreSQL | 16 (Alpine) | `docker-compose.yml:3` |
| Docker / Docker Compose | v2 | `docker-compose.yml` |
| Flyway | Spring Boot managed | `pom.xml:43` |
| Spring Security | Spring Boot managed | `pom.xml:47` |
| Spring Web MVC | Spring Boot managed | `pom.xml:55` |
| Spring Data JPA | Spring Boot managed | `pom.xml:39` |
| Validation (Jakarta) | Spring Boot managed | `pom.xml:51` |
| Actuator + Micrometer | Spring Boot managed | `pom.xml:35` |
| ArchUnit | 1.4.1 | `pom.xml:106` |
| JUnit 5 | Spring Boot managed | `pom.xml:90-97` |
| PostgreSQL Driver | Spring Boot managed | `pom.xml:70` |

---

## Prerequisites (Ubuntu/Linux)

- **Java 25** (LTS) — `apt install openjdk-25-jdk` or use SDKMAN
- **Maven 3.8+** — Wrapper included (`./mvnw`), system Maven also works
- **Docker & Docker Compose v2** — `apt install docker.io docker-compose-plugin`
- **Git** — `apt install git`

Verify installations:
```bash
java --version
mvn --version
docker --version
docker compose version
git --version
```

---

## Repository Structure

```
tech-kernel-backend/
├── docs/
│   ├── architecture/
│   │   ├── overview.md              # High-level architecture + Mermaid diagram
│   │   ├── modules.md               # Module responsibilities & dependency matrix
│   │   ├── rbac.md                  # RBAC model, roles, domain-scoped DOMAIN_LEAD
│   │   ├── dependency-rules.md      # Module boundaries, cross-module communication
│   │   ├── api-conventions.md       # REST conventions (naming, HTTP, DTOs, errors)
│   │   ├── testing.md               # Test pyramid, conventions per layer
│   │   ├── implementation-guidelines.md # SOLID, layering, anti-patterns, code style
│   │   ├── mvp-feature-map.md       # Feature → module mapping, persistence, APIs
│   │   └── [domain-model.md]        # (future) Conceptual domain model
│   └── setup.md                     # (future) Detailed onboarding guide
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── mittechkernel/
│   │   │           └── backend/
│   │   │               ├── TechKernelApplication.java
│   │   │               ├── config/              # Spring @Configuration classes
│   │   │               ├── security/            # Security config, filters, handlers
│   │   │               ├── common/
│   │   │               │   ├── exception/       # Global exceptions, base classes
│   │   │               │   ├── response/        # ApiResponse, PageResponse wrappers
│   │   │               │   └── util/            # Generic utilities (no business logic)
│   │   │               └── modules/             # Feature modules (16 planned)
│   │   │                   ├── auth/
│   │   │                   ├── user/
│   │   │                   ├── member/
│   │   │                   ├── domain/
│   │   │                   ├── event/           # (planned rename: session)
│   │   │                   ├── registration/
│   │   │                   ├── foundation/
│   │   │                   ├── leaderboard/
│   │   │                   ├── resource/        # (planned)
│   │   │                   ├── task/            # (planned)
│   │   │                   ├── project/         # (planned)
│   │   │                   ├── skill/           # (planned)
│   │   │                   ├── gamification/    # (planned)
│   │   │                   ├── notification/    # (planned)
│   │   │                   ├── recommendation/  # (planned)
│   │   │                   ├── analytics/       # (planned)
│   │   │                   └── audit/           # (planned)
│   │   └── resources/
│   │       ├── application.yml           # Base configuration
│   │       ├── application-dev.yml       # Development profile
│   │       ├── application-prod.yml      # Production profile
│   │       └── db/
│   │           └── migration/            # Flyway SQL migrations V1–V17
│   └── test/
│       └── java/
│           └── com/
│               └── mittechkernel/
│                   └── backend/
│                       ├── TechKernelApplicationTests.java
│                       └── ArchitectureTests.java      # ArchUnit boundary tests
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── .gitignore
```

**Note**: The `modules/` package currently contains 8 modules (auth, user, member, domain, event, registration, foundation, leaderboard). The MVP feature map defines 17 total modules; the additional 9 are planned and documented in `docs/architecture/mvp-feature-map.md`.

---

## Architecture

### Modular Architecture
The backend follows a modular monolith pattern. Each business capability lives in its own module under `com.mittechkernel.backend.modules.*`. Modules are independently developable and testable.

### Layered Responsibilities
```
Controller (HTTP, validation, serialization)
    ↓
Service (business logic, orchestration, @Transactional)
    ↓
Repository (persistence access, custom queries)
    ↓
Entity (JPA persistence model)
```
DTOs are used at API boundaries. Entities never leak across module boundaries.

### Module Boundaries
- **Business modules** (`modules/*`) may depend on **shared infrastructure** (`common/`, `config/`, `security/`) freely
- **Shared infrastructure** MUST NOT depend on business modules
- **Cross-module reads** happen ONLY through **QueryService interfaces** (e.g., `UserQueryService`)
- **Cross-module writes** happen via **domain events** (published to `audit`, `gamification`, `notification`, `analytics`)
- No module accesses another module's JPA entities or repositories directly

### RBAC Model
Five system-wide roles: `SUPER_ADMIN`, `CORE_MEMBER`, `FACULTY`, `DOMAIN_LEAD`, `STUDENT`.

**Critical**: `DOMAIN_LEAD` is **domain-scoped** — permissions apply only to explicitly assigned technical domains. It is NOT a globally privileged role.

### Foundation Program
The Foundation Program is a **separate program/scope**, NOT an RBAC role. It has its own internal roles (`PARTICIPANT`, `MENTOR`, `COORDINATOR`) managed within the `foundation` module. A user can be a `STUDENT` in RBAC AND a Foundation participant simultaneously.

### Architecture Tests
ArchUnit tests enforce boundaries at build time. See `src/test/java/com/mittechkernel/backend/ArchitectureTests.java`. Run with:
```bash
mvn test -Dtest=ArchitectureTests
```

### Detailed Documentation
See `docs/architecture/` for:
- [Overview](docs/architecture/overview.md)
- [Module Responsibilities](docs/architecture/modules.md)
- [RBAC Model](docs/architecture/rbac.md)
- [Dependency Rules](docs/architecture/dependency-rules.md)
- [API Conventions](docs/architecture/api-conventions.md)
- [Testing Conventions](docs/architecture/testing.md)
- [Implementation Guidelines](docs/architecture/implementation-guidelines.md)
- [MVP Feature Map](docs/architecture/mvp-feature-map.md)

---

## Local Setup — Ubuntu/Linux

### 1. Clone the Repository
```bash
git clone <repository-url>
cd tech-kernel-backend
```

### 2. Configure Environment Variables
Create a `.env` file in the project root (this file is git-ignored):
```bash
cat > .env << 'EOF'
# PostgreSQL (Docker Compose)
POSTGRES_DB=tech_kernel
POSTGRES_USER=tech_kernel
POSTGRES_PASSWORD=changeme

# Spring Boot (used by application.yml)
DATABASE_URL=jdbc:postgresql://localhost:5433/tech_kernel
DATABASE_USERNAME=tech_kernel
DATABASE_PASSWORD=changeme
EOF
```

### 3. Start PostgreSQL
```bash
docker compose up -d
```

### 4. Verify PostgreSQL Container
```bash
docker compose ps
docker compose logs -f postgres
```
Wait for the health check to pass (shows `healthy`).

### 5. Start the Spring Boot Backend (Development Profile)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
Or with system Maven:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application starts on **port 8080** with the `dev` profile active.

---

## Environment Variables

| Variable | Required | Purpose | Default (dev) |
|----------|----------|---------|---------------|
| `POSTGRES_DB` | Yes | PostgreSQL database name | `tech_kernel` |
| `POSTGRES_USER` | Yes | PostgreSQL username | `tech_kernel` |
| `POSTGRES_PASSWORD` | Yes | PostgreSQL password | `changeme` |
| `DATABASE_URL` | Yes | JDBC URL for Spring Boot | `jdbc:postgresql://localhost:5433/tech_kernel` |
| `DATABASE_USERNAME` | Yes | DB username for Spring Boot | `tech_kernel` |
| `DATABASE_PASSWORD` | Yes | DB password for Spring Boot | `changeme` |
| `PORT` | No | HTTP server port; Render supplies this for web services | `8080` |

All variables are read from the environment (or `.env` via Docker Compose). **Never commit `.env` or real secrets.**

---

## Database

- **Engine**: PostgreSQL 16 (Alpine)
- **Container**: `tech-kernel-postgres` (via Docker Compose)
- **Host Port**: 5433 → Container Port: 5432
- **Volume**: `postgres_data` (persistent, survives container restart)
- **Flyway**: Enabled, migrations from `classpath:db/migration`, Hibernate uses `ddl-auto: validate`
- **Current State**: Migrations V1–V17 define the application schema. V8 adds reference domains/achievements; V9 and V10 are explicitly identified demo-user and walkthrough data seeds. Review [Production deployment audit and Supabase setup](docs/PRODUCTION-DEPLOYMENT-AUDIT.md) before using a production database.
- **Production safety**: Flyway clean is disabled, SSL is required by default, and production will not silently baseline an existing schema. Existing database history must be inspected before first startup.
- **Hibernate DDL**: `validate` (no auto-DDL in production)

### Database Commands
```bash
# Start
docker compose up -d

# Stop (preserves data)
docker compose down

# Stop and remove volume (DATA LOSS)
docker compose down -v

# View logs
docker compose logs -f postgres

# Connect via psql
docker exec -it tech-kernel-postgres psql -U tech_kernel -d tech_kernel
```

**Important**: The project's PostgreSQL runs on **host port 5433** to avoid conflict with any unrelated PostgreSQL installation on the host (typically port 5432).

---

## Running the Application

### Development Profile (with SQL logging, debug output)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Profile
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Package as JAR
```bash
./mvnw package
```

### Run Packaged JAR
```bash
java -jar target/tech-kernel-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Testing

```bash
# Run all tests (unit + integration + ArchUnit)
./mvnw test

# Run only ArchUnit architecture tests
./mvnw test -Dtest=ArchitectureTests

# Run only Spring context test
./mvnw test -Dtest=TechKernelApplicationTests

# Compile only (no tests)
./mvnw compile

# Compile tests
./mvnw test-compile
```

---

## Health Check

Actuator health endpoint (exposed on port 8080):
```bash
curl -s http://localhost:8080/actuator/health | jq .
```

Expected response when healthy:
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

Other Actuator endpoints (require authentication in dev):
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

---

## Development Workflow

```bash
# 1. Make changes
# 2. Run tests
./mvnw test

# 3. Verify compilation
./mvnw compile

# 4. Inspect changes
git diff
git status

# 5. Commit (follow conventional commits)
git add .
git commit -m "feat(module): description"
```

### Commit Message Convention
```
<type>(<scope>): <subject>

<body>

<footer>
```
Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`

---

## Architecture & Development Rules

### Core Principles
- **SOLID** — Single responsibility, open/closed, Liskov substitution, interface segregation, dependency inversion
- **Constructor Injection** — Use `@RequiredArgsConstructor` on all Spring beans; no field injection (`@Autowired` on fields)
- **Layer Separation** — Controllers handle HTTP; services contain business logic; repositories handle persistence; entities are persistence models only
- **No Entity Leakage** — Entities never cross module boundaries or appear in API responses
- **No Circular Dependencies** — Enforced by ArchUnit tests
- **Module Boundaries** — Enforced by ArchUnit; cross-module reads via QueryService interfaces only
- **No Secrets Committed** — `.env` and `.env.*` are git-ignored; use environment variables

### Anti-Patterns to Avoid
- God classes / god services
- Field injection
- Business logic in controllers or repositories
- Premature abstractions / unnecessary interfaces
- Static global state
- Duplicated business logic
- Leaking persistence entities through APIs

---

## Current Implementation Status

### Current implementation

This README began as a scaffold document and some architecture notes below may
describe an earlier planned state. The repository now contains authentication,
authorization, account administration, projects, sessions, resources, tasks,
attendance, gamification, GitHub integration, and Flyway migrations V1–V17.
Use the source tree and the current feature/operations documents as the source
of truth. The V9/V10 demo seeds are a production deployment blocker; see the
production audit before pointing the backend at a live database.

### 📋 Documented (Architecture & Design)
- Module responsibilities & dependency matrix
- RBAC conceptual model (5 roles, domain-scoped DOMAIN_LEAD)
- Cross-module dependency rules & communication patterns
- REST API conventions (naming, HTTP methods, DTOs, pagination, errors)
- Testing conventions (unit, integration, controller, security, ArchUnit)
- Implementation guidelines (SOLID, layering, code style, git conventions)
- MVP feature → module mapping (17 modules, 19 feature areas)
- Conceptual domain model (planned)
- Database design principles (planned)

### ❌ NOT Implemented (Planned MVP Features)
- **All business entities** (User, Session, Task, Project, Resource, Skill, etc.)
- **All repositories, services, controllers**
- **All Flyway business migrations** (only baseline exists)
- **Authentication / JWT / RBAC enforcement**
- **Any business logic or API endpoints**
- **Seed data**
- **Resource library, tasks, projects, skills, gamification, notifications, analytics, audit**
- **Personalized recommendations engine**

---

## Documentation Index

| Document | Path |
|----------|------|
| Render deployment | `docs/RENDER-DEPLOYMENT.md` |
| Production deployment audit and Supabase setup | `docs/PRODUCTION-DEPLOYMENT-AUDIT.md` |
| Architecture Overview | `docs/architecture/overview.md` |
| Module Responsibilities | `docs/architecture/modules.md` |
| RBAC Model | `docs/architecture/rbac.md` |
| Dependency Rules | `docs/architecture/dependency-rules.md` |
| API Conventions | `docs/architecture/api-conventions.md` |
| Testing Conventions | `docs/architecture/testing.md` |
| Implementation Guidelines | `docs/architecture/implementation-guidelines.md` |
| MVP Feature Map | `docs/architecture/mvp-feature-map.md` |
| Detailed Setup Guide | `docs/setup.md` (planned) |

---

## Troubleshooting

### Docker Permission Issues
```bash
# If "permission denied" on docker.sock:
sudo usermod -aG docker $USER
newgrp docker
# Or run docker compose with sudo (not recommended for regular use)
```

### PostgreSQL Container Won't Start
```bash
# Check if port 5433 is already in use
sudo lsof -i :5433

# Check container logs
docker compose logs postgres

# Remove stale container/volume and retry
docker compose down -v
docker compose up -d
```

### Environment Variables Not Loading
- Ensure `.env` file exists in project root
- Verify variable names match exactly (case-sensitive)
- Docker Compose reads `.env` automatically; Spring Boot reads from system environment

### Java/Maven Version Mismatch
```bash
# Verify Java 25
java --version

# If wrong version, set JAVA_HOME or use SDKMAN
sdk use java 25.0.4-oracle  # or your installed version

# Verify Maven uses correct Java
./mvnw -version
```

### Port Conflicts
- **8080** — local Spring Boot fallback port (Render supplies `PORT`)
- **5433** — PostgreSQL host port (change in `docker-compose.yml` and `DATABASE_URL`)

### Tests Failing
```bash
# Clean and rebuild
./mvnw clean test

# Check database connectivity (PostgreSQL must be running)
docker compose ps
```

### Architecture Tests Failing
```bash
# Run only ArchUnit tests to see violations
./mvnw test -Dtest=ArchitectureTests

# Common causes:
# - New module accessing another module's entities/repositories
# - common/ or config/ depending on modules/*
# - Missing QueryService interfaces for cross-module reads
```

---

## License
This project is proprietary to MIT TECH KERNEL.
