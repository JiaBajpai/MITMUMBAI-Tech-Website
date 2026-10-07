# MIT TECH KERNEL Backend - Module Responsibilities

## Module Overview

Each module under `com.mittechkernel.backend.modules` represents a distinct business capability with clear boundaries. Modules MUST NOT depend on each other directly. Cross-module communication happens through:
- Shared read models (DTOs in `common/response/` or dedicated contract packages)
- Domain events (future consideration)
- Explicit service-to-service calls via well-defined interfaces (avoid where possible)

---

## auth Module

**Package**: `com.mittechkernel.backend.modules.auth`

### Responsibilities
- Authentication (login, logout, credential validation)
- Session management (JWT token issuance, refresh, revocation)
- Password management (reset, change, strength policy)
- Identity provider integration (future: OAuth2, SAML)
- Account lockout / brute-force protection
- Multi-factor authentication (future)

### Owns
- `UserCredentials` entity (hashed passwords, salt)
- `RefreshToken` / `Session` entities
- `AuthenticationAttempt` audit records

### Exposes
- `AuthenticationService` - authenticate(), refreshToken(), logout()
- `TokenService` - generateAccessToken(), validateToken(), revokeToken()
- `PasswordService` - encode(), matches(), reset()

### Does NOT Own
- User profile data (→ `user` module)
- Membership data (→ `member` module)
- Role assignments (→ `user` module via `UserRole`)

### Dependencies
- `common/exception` - `AuthenticationException`, `TokenExpiredException`
- `common/response` - `ApiResponse`, `TokenResponse`
- `common/util` - `SecurityUtils`, `JwtUtils`
- `security` - Spring Security integration points

---

## user Module

**Package**: `com.mittechkernel.backend.modules.user`

### Responsibilities
- Core user identity (student ID, email, name, contact info)
- Account lifecycle (registration, activation, deactivation, deletion)
- Profile management (avatar, bio, preferences)
- Role assignment (RBAC roles: SUPER_ADMIN, CORE_MEMBER, FACULTY, DOMAIN_LEAD, STUDENT)
- Domain lead assignments (mapping user ↔ domain)
- Account verification (email, student ID)

### Owns
- `User` entity (core identity)
- `UserProfile` entity (extended profile)
- `UserRole` entity (role assignments)
- `DomainLeadAssignment` entity (user ↔ domain mapping)

### Exposes
- `UserService` - create(), update(), deactivate(), assignRole(), assignDomainLead()
- `UserQueryService` - findById(), findByEmail(), findByStudentId(), findByRole()
- `ProfileService` - updateProfile(), updatePreferences()

### Does NOT Own
- Authentication credentials (→ `auth` module)
- Membership-specific data (→ `member` module)
- Event registrations (→ `registration` module)
- Leaderboard scores (→ `leaderboard` module)

### Dependencies
- `common/exception` - `UserNotFoundException`, `DuplicateUserException`
- `common/response` - `UserResponse`, `UserProfileResponse`
- `common/util` - `ValidationUtils`

---

## member Module

**Package**: `com.mittechkernel.backend.modules.member`

### Responsibilities
- Club membership lifecycle (application, approval, renewal, expiry)
- Member-specific metadata (batch, year, domain interests, skills)
- Membership fees / payment tracking (future)
- Member directory (search, filter)
- Alumni status tracking

### Owns
- `Member` entity (membership-specific data)
- `MemberApplication` entity (application workflow)
- `MembershipPeriod` entity (active periods)

### Exposes
- `MemberService` - apply(), approve(), reject(), renew(), expire()
- `MemberQueryService` - findByUserId(), findByBatch(), findByDomainInterest(), search()
- `MembershipService` - getActiveMembers(), getExpiringSoon()

### Does NOT Own
- Core user identity (→ `user` module)
- Authentication (→ `auth` module)
- Event participation (→ `registration` module)

### Dependencies
- `user` module - reads `User` for membership eligibility (via UserQueryService interface)
- `common/exception` - `MemberNotFoundException`, `ApplicationNotFoundException`
- `common/response` - `MemberResponse`, `MemberApplicationResponse`

---

## domain Module

**Package**: `com.mittechkernel.backend.modules.domain`

### Responsibilities
- Technical domain definitions (AI/ML, Web Dev, Cybersecurity, etc.)
- Domain metadata (description, logo, color, display order)
- Domain lead management (assignment, responsibilities)
- Domain-member relationships (which members belong to which domain)
- Domain-specific settings/configuration

### Owns
- `Domain` entity (domain definition)
- `DomainLead` entity (lead assignments with term dates)
- `DomainMember` entity (member ↔ domain association)
- `DomainSettings` entity (domain-specific configuration)

### Exposes
- `DomainService` - create(), update(), assignLead(), addMember()
- `DomainQueryService` - findAll(), findById(), findByLead(), findByMember()
- `DomainLeadService` - getCurrentLead(), getLeadHistory()

### Does NOT Own
- User accounts (→ `user` module)
- Events (→ `event` module, though events belong to a domain)
- Leaderboards (→ `leaderboard` module, though leaderboards are per domain)

### Dependencies
- `user` module - reads `User` for domain lead validation
- `common/exception` - `DomainNotFoundException`, `DomainLeadConflictException`
- `common/response` - `DomainResponse`, `DomainLeadResponse`

---

## session Module

**Package**: `com.mittechkernel.backend.modules.session` (planned rename from `event`)

### Responsibilities
- Session definitions (title, description, date, venue, capacity)
- Session types (workshop, hackathon, talk, social, foundation)
- Session-domain association (each session belongs to one domain)
- Session configuration (registration open/close dates, waitlist, approval mode)
- Session visibility (public, members-only, domain-only, invite-only)
- Session metadata (speakers, sponsors, resources, feedback form)

### Owns
- `Session` entity (session definition)
- `SessionConfiguration` entity (registration settings)
- `SessionSpeaker` entity (speaker details)
- `SessionResource` entity (slides, recordings, links)

### Exposes
- `SessionService` - create(), update(), publish(), cancel(), configure()
- `SessionQueryService` - findById(), findByDomain(), findUpcoming(), findPast(), search()
- `SessionConfigurationService` - updateRegistrationSettings(), updateVisibility()

### Does NOT Own
- Registrations (→ `registration` module)
- User accounts (→ `user` module)
- Leaderboards (→ `leaderboard` module)

### Dependencies
- `domain` module - reads `Domain` for session-domain association
- `common/exception` - `SessionNotFoundException`, `SessionCapacityExceededException`
- `common/response` - `SessionResponse`, `SessionConfigurationResponse`

---

## registration Module

**Package**: `com.mittechkernel.backend.modules.registration`

### Responsibilities
- Registration workflow (apply → pending → confirmed/waitlisted/rejected)
- Registration state machine
- Waitlist management (auto-promotion, position tracking)
- Attendance tracking (check-in, check-out, no-show)
- Registration cancellation (with policy enforcement)
- Registration limits (per user, per event, per domain)
- Registration questions / custom fields

### Owns
- `Registration` entity (registration record with state)
- `WaitlistEntry` entity (waitlist position, promoted timestamp)
- `Attendance` entity (check-in/out records)
- `RegistrationQuestion` / `RegistrationAnswer` entities

### Exposes
- `RegistrationService` - register(), cancel(), confirm(), reject(), waitlist()
- `AttendanceService` - checkIn(), checkOut(), getAttendanceReport()
- `RegistrationQueryService` - findByUser(), findByEvent(), findByStatus(), getWaitlistPosition()

### Does NOT Own
- Event definitions (→ `event` module)
- User accounts (→ `user` module)
- Member data (→ `member` module)

### Dependencies
- `event` module - reads `Event` for capacity, dates, configuration
- `user` module - reads `User` for registration eligibility
- `common/exception` - `RegistrationNotFoundException`, `RegistrationClosedException`, `WaitlistFullException`
- `common/response` - `RegistrationResponse`, `WaitlistResponse`, `AttendanceResponse`

---

## foundation Module

**Package**: `com.mittechkernel.backend.modules.foundation`

### Responsibilities
- Foundation Program curriculum (phases, milestones, tasks)
- Participant enrollment & progress tracking
- Task/submission management (assign, submit, review, grade)
- Mentor-mentee pairing
- Program-level analytics (completion rates, dropout)
- Certificate generation (future)

### Key Distinction
**Foundation Program is a separate program/scope, NOT an RBAC role.** A user can be a STUDENT in RBAC terms AND a Foundation Program participant simultaneously. The program has its own internal roles (participant, mentor, coordinator) that are distinct from system-wide RBAC roles.

### Owns
- `FoundationProgram` entity (program definition, cohort)
- `FoundationPhase` entity (phase definition, order, requirements)
- `FoundationTask` entity (task definition, type, deadline)
- `FoundationEnrollment` entity (user ↔ program enrollment)
- `FoundationSubmission` entity (submission, grade, feedback)
- `FoundationMentorship` entity (mentor ↔ mentee pairing)

### Exposes
- `FoundationProgramService` - createProgram(), enroll(), advancePhase()
- `FoundationTaskService` - createTask(), submit(), review(), grade()
- `FoundationQueryService` - getProgress(), getSubmissions(), getMentorship()
- `FoundationAnalyticsService` - getCompletionStats(), getDropoutRate()

### Does NOT Own
- User accounts (→ `user` module)
- Events (→ `event` module, though Foundation may have associated events)
- Leaderboards (→ `leaderboard` module, though Foundation may contribute to scores)

### Dependencies
- `user` module - reads `User` for enrollment eligibility
- `event` module - reads `Event` for Foundation-associated events (optional)
- `common/exception` - `FoundationNotFoundException`, `EnrollmentNotFoundException`, `TaskNotFoundException`
- `common/response` - `FoundationResponse`, `TaskResponse`, `ProgressResponse`

---

## leaderboard Module

**Package**: `com.mittechkernel.backend.modules.leaderboard`

### Responsibilities
- Ranking calculations (points, weighted scores, custom formulas)
- Leaderboard types (global, per-domain, per-event, per-foundation-cohort)
- Score sources (event attendance, task completion, peer votes, admin adjustments)
- Ranking periods (all-time, seasonal, monthly, per-event)
- Tie-breaking rules
- Historical snapshots (for trend analysis)
- Public/private visibility controls

### Owns
- `Leaderboard` entity (leaderboard definition, type, period)
- `LeaderboardEntry` entity (user, score, rank, calculated_at)
- `ScoreEvent` entity (source event: attendance, submission, vote, adjustment)
- `ScoreRule` entity (scoring formula configuration)

### Exposes
- `LeaderboardService` - calculate(), recalculate(), adjustScore(), snapshot()
- `LeaderboardQueryService` - getLeaderboard(), getUserRank(), getHistory(), getTopN()
- `ScoreEventService` - recordAttendance(), recordSubmission(), recordVote(), adjustManually()

### Does NOT Own
- User accounts (→ `user` module)
- Events (→ `event` module)
- Registrations (→ `registration` module)
- Foundation tasks (→ `foundation` module)

### Dependencies
- `user` module - reads `User` for display info
- `session` module - reads `Session` for session-based scores
- `registration` module - reads `Registration`/`Attendance` for attendance scores
- `foundation` module - reads `FoundationSubmission` for task scores
- `domain` module - reads `Domain` for domain-scoped leaderboards
- `common/exception` - `LeaderboardNotFoundException`, `ScoreCalculationException`
- `common/response` - `LeaderboardResponse`, `LeaderboardEntryResponse`, `UserRankResponse`

---

## Summary: Module Dependency Matrix

| Module | Depends On (Read-Only) |
|--------|------------------------|
| auth | common, security |
| user | common |
| member | user (query), common |
| domain | user (query), common |
| session | domain (query), common |
| registration | session (query), user (query), common |
| foundation | user (query), session (query, optional), common |
| leaderboard | user (query), session (query), registration (query), foundation (query), domain (query), common |

**Rule**: Modules only read from other modules via query services. They NEVER modify another module's data directly.