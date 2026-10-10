# Backend API integration reference

This reference describes routes implemented in the backend source as of 2026-10-09. It is based on controllers, DTOs, services, and PostgreSQL integration tests. There is no generated OpenAPI specification.

## Status and environment

- Java 25, Spring Boot 4.1.1, PostgreSQL 16, Flyway V1–V14.
- API base path: /api/v1.
- Send Authorization: Bearer <accessToken> to authenticated routes.
- Access token lifetime defaults to 15 minutes; refresh token lifetime defaults to 7 days. Both are configurable.
- No registration, user administration, session management, resource write, notification, or Foundation enrollment routes are implemented.
- Five /api/v1/test/** authorization diagnostic routes are implemented; they are not product workflows.

“Implemented and tested” means an integration test exercises the route or feature path. It does not mean every validation and authorization branch has a separate test. Integration tests use PostgreSQL; GitHub HTTP calls are stubbed.

## Responses and errors

Most successes use this envelope: { timestamp, status, success, data, error, message, path }. API exceptions use the same envelope with success=false, data=null, plus an error code and message. Common statuses: 400 invalid input/state, 401 missing or invalid credentials, 403 authorization failure, 404 missing record, 502 GitHub upstream failure. Bean validation errors return 400 and field messages in data. Framework-level errors and 204 responses are not always enveloped. Production suppresses framework error and binding messages.

## Endpoint inventory

Routes below omit the shared /api/v1 prefix unless the Actuator path is shown. Authenticated means any active user with a valid access token. /me identity comes from the authenticated token and cannot be selected by the client.

### Authentication and current user

| Method and path | Request | Response | Authorization | Status and test |
|---|---|---|---|---|
| POST /auth/login | LoginRequest: email, password | 200 AuthSuccessResponse: accessToken, refreshToken, user | Public; inactive accounts rejected | Implemented and tested: AuthFlowIntegrationTest |
| POST /auth/refresh | RefreshTokenRequest: refreshToken | 200 AuthSuccessResponse; old refresh token consumed | Public; token must be signed, unexpired, stored, unused | Implemented and tested: AuthFlowIntegrationTest |
| POST /auth/logout | RefreshTokenRequest; optional body, bearer refresh token fallback | 200 envelope, data=null | Public; matching stored token is revoked; unknown token is an idempotent no-op | Implemented and tested: AuthFlowIntegrationTest |
| GET /auth/me | — | 200 UserPublicResponse | Authenticated; current account | Implemented and tested: AuthFlowIntegrationTest, RbacIntegrationTest |
| GET /test/student | — | 200 student-ok | STUDENT | Implemented and tested: RbacIntegrationTest; diagnostic only |
| GET /test/core-member | — | 200 core-member-ok | CORE_MEMBER | Implemented and tested: RbacIntegrationTest; diagnostic only |
| GET /test/faculty | — | 200 faculty-ok | FACULTY | Implemented and tested: RbacIntegrationTest; diagnostic only |
| GET /test/system | — | 200 system-ok | SUPER_ADMIN | Implemented and tested: RbacIntegrationTest; diagnostic only |
| GET /test/domain?domainId={id} | — | 200 domain-access-ok:{id} | DOMAIN_LEAD assigned to that active domain | Implemented and tested: RbacIntegrationTest; diagnostic only |
| GET /me/profile | — | 200 UserProfileResponse | Authenticated; own account only | Implemented and tested: UserProfileApiIntegrationTest |
| PATCH /me/profile | UserProfileUpdateRequest: bio?, avatarUrl?, githubUrl?, linkedinUrl?, phone? | 200 UserProfileResponse | Authenticated; mutable fields only | Implemented and tested: UserProfileApiIntegrationTest |
| GET /me/dashboard | — | 200 DashboardResponse | Authenticated; current account only | Implemented and tested: DashboardApiIntegrationTest, FoundationProgramIntegrationTest |

UserPublicResponse fields: id, name, email, program, roles. UserProfileResponse adds bio, avatarUrl, githubUrl, linkedinUrl, phone. These DTOs do not include passwords, JWTs, GitHub credentials, or encryption data. Profile PATCH cannot change identity, role, program, or email.

### Domains, resources, sessions, attendance, and tasks

| Method and path | Query/body | Response | Authorization | Status and test |
|---|---|---|---|---|
| GET /domains | — | 200 DomainResponse[]: id, name, description, displayOrder | Authenticated | Implemented and tested: DomainApiIntegrationTest |
| GET /domains/{id} | — | 200 DomainResponse | Assigned DOMAIN_LEAD only | Implemented and tested: DomainApiIntegrationTest |
| GET /resources | domainId?, type?, difficulty?, topic? | 200 ResourceResponse[] | Authenticated; Domain Leads restricted to assigned domains | Implemented and tested: ResourceApiIntegrationTest |
| GET /resources/{id} | — | 200 ResourceResponse | Authenticated; Domain Leads restricted to assigned domain | Implemented and tested: ResourceApiIntegrationTest |
| GET /sessions | domainId?, program?, type?, date? (YYYY-MM-DD) | 200 SessionResponse[] | Authenticated; Domain Leads see assigned domains | Implemented and tested: SessionAttendanceApiIntegrationTest, FoundationProgramIntegrationTest |
| GET /sessions/{id} | — | 200 SessionResponse | Authenticated; Domain Leads restricted to assigned domain | Implemented and tested: SessionAttendanceApiIntegrationTest, FoundationProgramIntegrationTest |
| GET /sessions/{id}/attendance | — | 200 AttendanceResponse[] | Super Admin/Core Member, session lead, or technical Domain Lead for session domain. Faculty cannot view attendance here. | Implemented and tested: SessionAttendanceApiIntegrationTest, FoundationProgramIntegrationTest |
| POST /sessions/{id}/attendance | AttendanceRequest: userId, status | 201 AttendanceResponse | Same attendance managers; participant program must match session; duplicate attendance rejected | Implemented and tested: SessionAttendanceApiIntegrationTest, FoundationProgramIntegrationTest |
| GET /me/attendance | — | 200 AttendanceResponse[] | Authenticated; own records only | Implemented and tested: SessionAttendanceApiIntegrationTest |
| GET /tasks | sessionId? | 200 TaskResponse[] | Authenticated; Domain Leads see tasks in assigned domains | Implemented and tested: TaskApiIntegrationTest, FoundationProgramIntegrationTest |
| GET /tasks/{id} | — | 200 TaskResponse | Authenticated; Domain Leads restricted to assigned domain | Implemented and tested: TaskApiIntegrationTest |
| POST /tasks | TaskRequest: sessionId, projectId?, title, requirements?, deadline?, verificationRequired?, assigneeId?, status? | 201 TaskResponse | Super Admin/Core Member or assigned technical Domain Lead; Foundation task creation is privileged-only; assignee program must match session; new status is OPEN | Implemented and tested: TaskApiIntegrationTest, FoundationProgramIntegrationTest |
| PATCH /tasks/{id} | Partial TaskRequest | 200 TaskResponse | Super Admin/Core Member or assigned technical Domain Lead; assignees and Faculty cannot edit; status changes use completion/verification routes | Implemented and tested: TaskApiIntegrationTest, FoundationProgramIntegrationTest |
| POST /tasks/{id}/complete | TaskCompletionRequest: repoUrl, commitSha?, notes? | 200 TaskResponse | Assignee or privileged role for technical task; programs must match. Foundation completion is limited to assigned Foundation participant. | Implemented and tested: TaskApiIntegrationTest, FoundationProgramIntegrationTest |
| POST /tasks/{id}/verify | — | 200 TaskResponse | Super Admin/Core Member or assigned technical Domain Lead; Foundation verification is privileged-only | Implemented and tested: TaskApiIntegrationTest, FoundationProgramIntegrationTest |

SessionResponse fields: id, domainId, program, topic, type, date, time, description, objectives, instructor, leadId. Attendance status values: PRESENT, ABSENT, LATE, EXCUSED. AttendanceResponse fields: id, sessionId, userId, status, markedBy, createdAt.

TaskResponse fields: id, sessionId, projectId, title, requirements, deadline, verificationRequired, assigneeId, status. Task status values: OPEN, COMPLETED, VERIFIED. A verified task awards 15 XP. Verification-required tasks must match a persisted commit in the linked project repository; linked contributions must pass author identity, active membership, and program checks. A successful contribution also awards 40 XP.

### Projects and membership

| Method and path | Query/body | Response | Authorization | Status and test |
|---|---|---|---|---|
| GET /projects | domainId?, status?, program?, q? | 200 ProjectResponse[] | Authenticated; Domain Leads see assigned domains | Implemented and tested: ProjectApiIntegrationTest |
| GET /projects/{id} | — | 200 ProjectResponse | Authenticated; Domain Leads restricted to project domain | Implemented and tested: ProjectApiIntegrationTest |
| GET /projects/{id}/members | — | 200 ProjectMemberResponse[] | Authenticated; Domain Leads restricted to project domain | Implemented and tested: ProjectApiIntegrationTest |
| POST /projects | ProjectRequest create fields | 201 ProjectResponse | Authenticated; owner and project programs must match; Domain Leads restricted to assigned domain | Implemented and tested: ProjectApiIntegrationTest, GitHubContributionApiIntegrationTest |
| PATCH /projects/{id} | Partial ProjectRequest | 200 ProjectResponse | Owner, Super Admin/Core Member, or assigned Domain Lead. Review decisions and proposal activation require reviewer authority. | Implemented and tested: ProjectApiIntegrationTest |
| POST /projects/{id}/review | ProjectReviewRequest: decision, comment? | 200 ProjectResponse | Super Admin/Core Member or assigned Domain Lead; owner alone cannot review | Implemented and tested: ProjectApiIntegrationTest |
| POST /projects/{id}/join | — | 201 ProjectMemberResponse with REQUESTED status | Authenticated; programs must match; duplicate membership rejected | Implemented and tested: ProjectApiIntegrationTest, GitHubContributionApiIntegrationTest |
| POST /projects/{id}/members/{userId}/approve | — | 200 ProjectMemberResponse with ACTIVE status | Owner, privileged role, or assigned Domain Lead; cannot approve self; programs checked again | Implemented and tested: ProjectApiIntegrationTest, FoundationProgramIntegrationTest |
| POST /projects/{id}/members/{userId}/reject | — | 200 ProjectMemberResponse with REJECTED status | Same membership managers; cannot reject self | Implemented and tested: ProjectApiIntegrationTest |
| DELETE /projects/{id}/members/{userId} | — | 204 | Same membership managers; owner cannot be removed | Implemented and tested: ProjectApiIntegrationTest |
| DELETE /projects/{id}/leave | — | 204 | Authenticated member; owner cannot leave | Implemented and tested: ProjectApiIntegrationTest |

ProjectRequest fields: name, domainId, program?, problem, solution, technologies?, requiredSkills?, expectedMembers, outcome?, status?, reviewComment?. ProjectResponse adds id, createdBy, approvedBy, timestamps, and nested members. ProjectMemberResponse: id, projectId, userId, role, status, reviewedBy, createdAt. Membership states are REQUESTED, ACTIVE, REJECTED; ordinary repository access requires ACTIVE. Programs are TECHNICAL and FOUNDATION. No enrollment workflow exists.

### GitHub OAuth, repositories, and contributions

| Method and path | Query/body | Response | Authorization | Status and test |
|---|---|---|---|---|
| POST /me/github/connect | — | 200 GitHubConnectionResponse: provider, state, authorizationUrl | Authenticated; callback binds to initiating session and state | Implemented and tested with stub client |
| GET /me/github/status | — | 200 GitHubConnectionStatusResponse: provider, connected, username | Authenticated; own account | Implemented and tested with stub client |
| DELETE /me/github/connect | — | 204 | Authenticated; disconnect own account | Implemented and tested with stub client |
| GET /github/oauth/callback | code, state | 200 GitHubCallbackResponse: provider, status | Public route but requires matching session state | Implemented and tested with stub client |
| GET /me/github/repositories | — | 200 GitHubRepositoryResponse[] | Authenticated; own connected GitHub identity | Implemented and tested: ProjectGitHubRepositoryApiIntegrationTest |
| POST /projects/{id}/github/repository | GitHubRepositoryRequest: githubRepoId | 201 GitHubRepositoryResponse | Project owner; repository must be accessible through owner’s linked account | Implemented and tested: ProjectGitHubRepositoryApiIntegrationTest |
| GET /projects/{id}/github/repository | — | 200 GitHubRepositoryResponse | Owner, privileged role, or ACTIVE project member | Implemented and tested: ProjectGitHubRepositoryApiIntegrationTest |
| DELETE /projects/{id}/github/repository | — | 204 | Owner only; rejected if verified contribution history exists | Implemented and tested: ProjectGitHubRepositoryApiIntegrationTest, GitHubContributionApiIntegrationTest |
| GET /projects/{projectId}/github/contributions | — | 200 GitHubContributionResponse[] after sync | Owner, privileged role, or ACTIVE member; sync uses repository linker’s credential | Implemented and tested with stub client: GitHubContributionApiIntegrationTest |
| POST /projects/{projectId}/github/contributions/{contributionId}/verify | — | 200 GitHubContributionResponse | Super Admin/Core Member or assigned project Domain Lead; author identity, ACTIVE membership, and program alignment required | Implemented and tested with stub client |

GitHubRepositoryResponse fields: id, projectId, githubRepoId, ownerLogin, repoName, fullName, htmlUrl, visibility, linkedBy, linkedAt. GitHubContributionResponse fields: id, sha, message, url, authorLogin, committedAt, kernelUserId, verified, verifiedAt. Responses never contain token ciphertext or GitHub access tokens.

**Sync limit:** the GitHub client requests only page 1 with 100 commits. Repository listing also fetches at most one page of 100. Pagination beyond the first 100 is not implemented. Tests use a stub; no live GitHub API was called.

### XP, leaderboards, dashboard

| Method and path | Query/body | Response | Authorization | Status and test |
|---|---|---|---|---|
| GET /me/xp | — | 200 XpSummaryResponse: totalXp, breakdown | Authenticated; own ledger | Implemented and tested: TaskApiIntegrationTest, SessionAttendanceApiIntegrationTest |
| GET /leaderboard | program? (default TECHNICAL), page? (0), size? (10; 1–100) | 200 LeaderboardPage: entries, page, size, totalElements, totalPages | Authenticated; active users in selected program | Implemented and tested: LeaderboardApiIntegrationTest, FoundationProgramIntegrationTest |
| GET /leaderboard/domains/{domainId} | page?, size? (same limits) | 200 LeaderboardPage | Authenticated; Domain Leads must be assigned to domain | Implemented and tested: LeaderboardApiIntegrationTest |

Leaderboard entries are { rank, userId, name, program, totalXp }. SQL aggregates xp_events; there is no second XP source. Ties use lowercase name then user ID. Active zero-XP users appear on overall program boards. Domain boards include eligible technical domain members/leads and users with attributable technical activity. Foundation activity is excluded from technical-domain ranking. Foundation and technical program boards use program=FOUNDATION or program=TECHNICAL. The dashboard rank uses the same overall ranking logic.

Awards: verified task 15 XP, verified GitHub contribution 40 XP, PRESENT attendance 5 XP. Program/domain rankings exclude unverified and unattributable activities. V14’s unique activity index and transactional award paths prevent duplicate awards.

### Diagnostic and operational routes

| Method and path | Authorization | Status |
|---|---|---|
| GET /actuator/health | Public; details hidden unless authorized | Implemented; Actuator |
| /actuator/info and /actuator/metrics/** | Authenticated by application security | Implemented; configured, not API integration-tested |

No Prometheus registry dependency is present, so /actuator/prometheus is unavailable. No environment, configuration-properties, or heapdump Actuator endpoint is exposed.

## DTO and validation notes

- ProjectReviewRequest decision is required; accepted values: APPROVE, REJECT, CHANGES_REQUESTED, UNDER_REVIEW.
- GitHubRepositoryRequest.githubRepoId is required.
- TaskRequest requires sessionId and nonblank title on creation. Requirements are checked in the service. Status is not an editable field in the lifecycle.
- TaskCompletionRequest.repoUrl is nonblank; a commit SHA is required when verification is enabled and then matched to persisted repository activity.
- AttendanceRequest requires userId and status; database uniqueness prevents duplicate attendance for a session/user pair.
- UserProfileUpdateRequest has bounded bio/avatar/social URL sizes and a phone pattern.
- Project/session/resource/task lists and contribution sync are unpaginated. Only leaderboard routes have bounded page size.

## Implemented in database/services but no frontend route

Work items, project activities/messages, skills, achievements, notifications, and audit records have schema or service foundations but no controller routes for those workflows. Session/resource creation and updates are absent. User registration and role/domain assignment APIs are absent; current users are provisioned outside this API and migrations seed demo users. Foundation reuses users.program, sessions, tasks, attendance, and leaderboard logic; separate enrollment and mentor management are out of scope.

## Browser deployment configuration

The Vite development server is `http://localhost:5173`; Vite preview is `http://localhost:4173`. The frontend also supports opening either server through `127.0.0.1`, so the development allowlist includes those four exact origins. Do not use other host/port combinations. Production requires `CORS_ALLOWED_ORIGINS` to be set to a comma-separated list of exact deployed frontend origins; there is no production default. Set `VITE_API_BASE_URL` at frontend build time to the reachable backend API base, including `/api/v1`; without it, the frontend uses the local backend URL and will not reach a deployed backend. The production profile also requires DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, GITHUB_REDIRECT_URI, and GITHUB_TOKEN_ENCRYPTION_KEY. Use a strong JWT secret and Base64-encoded 256-bit GitHub encryption key. Never place backend credentials in frontend code.

## GitHub OAuth integration

The local GitHub OAuth flow uses the standard `https://github.com/login/oauth/authorize` and `https://github.com/login/oauth/access_token` endpoints. The development callback is `http://localhost:8080/api/v1/github/oauth/callback`; it must match the callback URL registered for the GitHub OAuth App exactly. Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` from that registered app. The app's homepage URL should be the frontend origin (normally `http://localhost:4173` when using Vite preview). Do not use wildcard callback matching.

Also configure `GITHUB_TOKEN_ENCRYPTION_KEY` as a Base64-encoded 32-byte key; generate one with `openssl rand -base64 32`. Keep it stable and private because it encrypts stored user tokens. Put local values in the ignored `backend/.env.local` file, restrict its permissions, and load it into the backend process with `set -a; . ./.env.local; set +a` before `./mvnw spring-boot:run`. The file is ignored by Git. Never put these values in frontend environment files, source control, logs, or chat. Missing OAuth credentials or an invalid encryption key cause the connect operation to return a clear service-unavailable response; no fake client ID is used.

`GITHUB_SCOPE` defaults to `read:user`, which is sufficient for user identity and does not request repository write access. Public repositories may be listed; private repository access is not requested. GitHub OAuth App scopes are coarse and `repo` includes write access, so do not add it just to make private repositories appear. If private repository access is a requirement, prefer a GitHub App with only the required repository permissions and selected repositories.

The callback verifies and consumes a cryptographically random state value stored in the server session and uses PKCE with the S256 challenge before exchanging the one-time authorization code. The access token is encrypted in the backend database and is never returned to the frontend. While the account is not connected, the UI does not call the protected repository-list endpoint. When connected, upstream repository errors are shown with a non-secret, actionable message.
