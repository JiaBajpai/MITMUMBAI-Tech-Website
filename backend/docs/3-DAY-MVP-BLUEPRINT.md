# Kernel Members Corner — 3-Day MVP Technical Blueprint

> Source of truth: `Kernel_Members_Corner_System_Design-1.pdf` (44 sections, LEARN → PRACTICE → BUILD → COLLABORATE → CONTRIBUTE).
> Status: Greenfield (only `docs/` exists). Planning only — no code.
> Constraint: Working MVP in 3 days > perfect architecture.

---

## 1. System Understanding (Phase 1)

### 1.1 Purpose

Authenticated internal platform for Kernel Club (MIT Mumbai) connecting:

```text
SESSION → RESOURCE → TASK → GITHUB → PROJECT → SKILL → RECOMMENDATION → XP → LEADERBOARD
```

Feedback loop: `LEARN → PRACTICE → BUILD → CONTRIBUTE → MEASURE → RECOMMEND → LEARN AGAIN`

### 1.2 Modules (from doc)

`auth · member · domain · program · session · attendance · resource · recommendation · task · github · project · chat · achievement · notification · leaderboard · audit · analytics`

### 1.3 Roles

```text
SUPER_ADMIN
 └── CORE_MEMBER (org operations, replaces Admin + Tech Secretary)
     ├── FACULTY (read-only oversight)
     ├── DOMAIN_LEAD (scoped to assigned domains, e.g. Prem → Backend + AI/ML/IoT)
     └── STUDENT
```

5 fixed domains: `General, Frontend, Backend, AI/ML/IoT, Competitive Programming`
Programs (scope, not domain): `TECHNICAL, FOUNDATION`

### 1.4 Explicit vs Inferred vs Flags

| Area | Explicit (doc) | Inference | FLAG / Decision |
|---|---|---|---|
| AuthZ | ROLE → PERMISSIONS → DOMAIN/PROGRAM scope | Need `user_domains` join table, service-layer check | Full 20-permission admin UI = POST-MVP. MVP enforces `ROLE + domain-scope`. |
| Attendance | Entity Student ↔ Session + status + timestamp | Statuses `PRESENT/ABSENT/LATE/EXCUSED`, UNIQUE(user,session) | No QR/biometric defined → manual marking by Lead/Core. |
| Resource reco | SESSION → PREFS → SEARCH EXTERNAL → FILTER/SCORE/RANK, weights 40/20/15/15/10 | — | External crawl infeasible in 3 days → curated DB + same weights. |
| GitHub verify | Verify repo/branch/commit/PR/author/timestamp | Store `repo_url + commit_sha` | No OAuth defined → public repos only, format-check + Lead verify for MVP. Live API = stretch. |
| Projects | PROPOSED → UNDER_REVIEW → APPROVED → ACTIVE → PAUSED → COMPLETED → ARCHIVED, plus CHANGES_REQUESTED loop | Single `projects` table with `status` covers proposal + official | Recommendations do NOT auto-join; join = request → approval. |
| Discussions | General / Domain / Project chat, REST first, WS later | MVP = `messages` table, project scope only, poll 10s | General/Domain chat = P2. |
| Foundation | PROGRAM/SCOPE with own leaderboard | `users.program` + `sessions/projects.program_id`, default TECHNICAL | Membership rule vague — assume `users.program` field. |
| Notifications | DB-backed initially | `notifications` table + `GET ?unread=true`, poll 30s | No push/SMTP/WS. |
| Analytics | SQL aggregation, not in-memory | Counts + rates only | No BI. |
| Security | Spring Security, hash, backend authz, audit, DTOs strip secrets | JWT access+refresh, BCrypt-12 | Never trust FE role. |

No real-time requirement for MVP. No MVP-critical external integration except optional GitHub public API.

---

## 2. Feature Priority Matrix (Phase 2)

P0 = demo-critical · P1 = simplified/stretch · P2 = optional · P3 = post-MVP

| Feature | Pri | MVP? | Complexity | Deps | Reason |
|---|---|---|---|---|---|
| Auth (register/login/JWT refresh/logout) + seeds | P0 | YES | M | DB | Gates everything |
| RBAC: roles + domain-scope + audit | P0 simplified | YES | M | Auth | BE is security boundary; full permission UI = P3 |
| Domains (5 seed) + Programs (2 seed) | P0 | YES | S | — | Static Flyway seed |
| Sessions CRUD + filter/paginate | P0 | YES | M | Auth, Domains | Core LEARN loop |
| Attendance mark + my-attendance | P0 | YES | S | Sessions | XP/skill input |
| Resources CRUD + session-resource reco V1 | P0 simplified | YES | M | Sessions | Weighted score, no external crawl |
| Tasks + assign + submit/complete | P0 | YES | M | Sessions | PRACTICE loop |
| GitHub evidence + format-check + Lead verify btn | P0 mock / P1 live | YES mock | S/M | Tasks | Live API needs tokens/rate-limits; mock satisfies demo |
| Proposal submit + Lead review → auto-create ACTIVE | P0 | YES | M | Auth | Single-table lifecycle |
| Project detail + members + join-request/approve | P0 | YES | M | Projects | Preserves ownership (§22) |
| Work items + status + progress calc | P0 | YES | M | Projects | `done/total`, no self-reported % |
| Activity timeline (declared vs verified) | P0 | YES | S | Projects/Tasks | Append-only feed |
| Discussions (REST, project scope) | P0 partial | YES | S | Projects | General/Domain = P2 |
| Project reco V1 deterministic + Why | P0 | YES | M | Learning context | Differentiator, 1 formula |
| Skill profile V1 derived | P0 simple | YES | S | Activity | Weighted counts, no ML |
| XP engine + 5 hardcoded achievements | P0 | YES | M | Spring Events | Gamification demo |
| Leaderboards (overall + per-domain SQL) | P0 | YES | S | XP | Foundation-only board = P1 |
| Notifications (DB + poll) | P0 basic | YES | S | Events | 8 triggers only |
| Audit logs (admin actions) | P0 basic | YES | S | All mutates | Append-only + admin list |
| Dashboards (student + core) | P0 | YES | M | All | `COUNT/SUM/GROUP BY` only |
| Search/filter/pagination + indexes | P0 | YES | S | All lists | `page/size/sort/q/domain/status` |
| Live GitHub API verification | P1 | STRETCH | M | Tasks | Deferred: mock is enough |
| Personalized learning reco (3 rules) | P1 | If time | S | Skills | `IF pg>60 AND redis<20 THEN redis` |
| Foundation filter + board / Faculty read-only | P1 | Simplified | S | Users | No report builder |
| Redis / WS chat / Kafka / ES / K8s / ML | P2/P3 | NO | M/H | — | Ops cost kills Day 3; doc defers WS; T.Query + SQL suffices |

---

## 3. 3-Day Execution Plan (Phase 3)

**Rule: freeze API contract Day 1 morning → FE builds on mocks while BE implements.**

### Day 1 — Foundation + Contract (critical path)

| # | Task | Team | Hrs | Parallel? | Done when |
|---|---|---|---|---|---|
| 1.1 | Scaffold SB 3.3 + Java 25 + Maven + Flyway + PG16 + Compose + Springdoc + exception handler + BaseEntity | BE | 3 | Yes w/ 1.4 | `GET /actuator/health` + migration runs |
| 1.2 | Auth: users, refresh_tokens, login/refresh/logout, BCrypt, JWT filter, seed 6 users + 5 domains + 2 programs | BE | 4 | No (blocks all) | Login returns tokens; 401/403 correct |
| 1.3 | RBAC helper `canManageDomain()` + domain check + audit writer | BE | 2 | No | Lead-Backend 200 on Backend, 403 on Frontend (test) |
| 1.4 | FE scaffold: Vite+React+TS+Router+Tailwind+T.Query+Axios+Zustand+RHF/Zod+Sonner, shell + login + guards + refresh interceptor | FE | 4 | Yes | Guarded `/dashboard` works on mock |
| 1.5 | **FREEZE contract (§6) + mock JSON** | Leads | 1 | No | Signed table |
| 1.6 | Sessions + Attendance + Resources CRUD + pagination | BE | 3 | Yes w/ FE pages | Swagger CRUD works |

### Day 2 — Core loops + Reco V1

| # | Task | Team | Hrs | Notes |
|---|---|---|---|---|
| 2.1 | Tasks + completions + work-items + progress + activity (Spring Events) | BE | 4 | Blocks 2.4 |
| 2.2 | Projects lifecycle + members/join + discussions + notifications + audit | BE | 4 | Blocks FE projects |
| 2.3 | Reco V1 + Skill V1 + XP + Achievements(5) + Leaderboards (sync Java) | BE | 5 | Demo highlight |
| 2.4 | FE: sessions/detail/attendance/tasks + projects/detail/join/workboard/messages | FE | 7 | Start on mocks Day 1 |
| 2.5 | FE: dashboard + leaderboard + bell (30s) + reco panel w/ Why | FE | 4 | Needs reco contract |
| 2.6 | Integration (CORS, DTO, pagination) | Both | 2 | Must finish Day 2 night |

### Day 3 — Harden + Demo

| # | Task | Team | Hrs |
|---|---|---|---|
| 3.1 | Proposal revise loop, Lead verify btn, 3 learning rules, foundation filter, faculty read-only, audit page | BE 3 + FE 3 | 6 |
| 3.2 | Tests: BE authz + reco/XP units + 1 MockMvc/controller; FE happy-path + 1 Playwright | Both | 3 |
| 3.3 | Docker compose + seed demo data (20 users, 10 sessions, 6 projects) + indexes | BE | 2 |
| 3.4 | Bugfix + rehearsal + fallback screenshots | Both | 3 |

**Critical path:** `1.1 → 1.2 → 1.3 → 1.6 → 2.1 → 2.2 → 2.3 → integration → docker/demo`

---

## 4. Requirement → Engineering Mapping (Phase 4)

| Feature | FE | State | BE module | API | Entities | AuthZ |
|---|---|---|---|---|---|---|
| Sessions | `/sessions`, `/:id`, Card/Table/Filter | Server + URL `?domain=&q=&page=` | `session` | `GET/POST /sessions`, `GET/PATCH/DELETE /sessions/{id}` | `sessions` | Create: Lead(assigned)/Core; Read: auth |
| Attendance | Mark button, My table | Server | `attendance` | `POST /sessions/{id}/attendance`, `GET /me/attendance` | `attendance` UNIQUE(user,session) | Mark: Lead/Core; view own/scoped; Event→XP+5 |
| Resources | List + RecoBadge | Server | `resource`, `recommendation` | `GET/POST /sessions/{id}/resources`, `GET .../resource-recommendations` | `resources` | Same as session |
| Tasks | Checklist + EvidenceForm | Server + RHF | `task` | `POST /sessions/{id}/tasks`, `POST /tasks/{id}/complete`, `POST .../verify` | `tasks`, `task_completions` | Assign: Lead; Complete: assignee; 409 if `verificationRequired && !sha` |
| Projects | `/projects`, `/:id`, ProposalForm, WorkBoard, Timeline, JoinBtn | Server + RHF | `project` | `POST /proposals`, `POST /projects/{id}/review`, `/join`, work-items CRUD | `projects`, `project_members`, `work_items`, `activities` | Propose: Student; Review: Lead(assigned)/Core; `@Transactional` |
| Discussions | MessageBox (poll 10s) | Server refetch | `chat` | `GET/POST /projects/{id}/messages` | `messages` scope=PROJECT | Member or Lead/Core; no WS |
| Project reco | RecoPanel + Why | Server | `recommendation` | `GET /me/project-recommendations?limit=5` | Derived | Own only; sync <100ms |
| Skills/XP | SkillBars, Badge, Grid | Server | `skill`, `gamification` | `GET /me/skills`, `/me/xp`, `/leaderboards*` | `skills`, `xp_events`, `achievements` | Own / public board; Spring Events sync |
| Notify/Audit/Analytics | Bell, `/admin/audit`, `/dashboard` | Server poll 30s | `notification`, `audit`, `analytics` | `GET /notifications`, `/admin/audit`, `/analytics/*` | `notifications`, `audit_logs` | Audit: Core/Super |

---

## 5. Backend Stack — Java Only (Phase 5)

| Tech | Ver | Why | MVP? | Verdict |
|---|---|---|---|---|
| Java 25 LTS | 25 | Records, LTS, SB3 support | YES | Backend build target |
| Spring Boot 3.3 + Web + Validation | 3.3 | REST, Bean Validation, Advice | YES | Core |
| Spring Security + JJWT | 6.x / 0.12 | JWT access 15m + refresh 7d rotation | YES | Must |
| Spring Data JPA + Hibernate | — | CRUD + EntityGraph (N+1) | YES | DTO projections for lists |
| PostgreSQL 16 | 16 | Primary store, ILIKE, JSONB | YES | Only DB (override doc MySQL) |
| Flyway | 10 | `V1__schema`, `V2__seed` | YES | Team sync |
| Springdoc OpenAPI | 2.x | Live contract for FE | YES | `/swagger-ui.html` |
| MapStruct / manual mapper | — | Entity↔DTO | YES-lite | Manual ok for ~15 entities |
| Spring Events + @Async pool | — | TaskCompleted → XP/Achieve/Notify/Skill | YES | Sync default; async notify only |
| Lombok | — | Boilerplate | OPT | If team knows it |
| JUnit5 + Mockito + MockMvc (+H2) | — | Authz + algo tests | YES | H2 unit, 2 PG repo tests max |
| Redis / Kafka / ES / K8s / ML libs | — | Cache/bus/search/scale | NO | No gain at <10k rows; kills Day 3; leave `CacheService` stub |
| Maven + Docker Compose | 3.9 | Build + `pg+api+fe` demo | YES | Wrapper + `compose up` |

Jobs: `ThreadPoolTaskExecutor` + `@Scheduled` (deadline reminders). Search: `ILIKE + B-tree`. ES = P3.

---

## 6. Frontend Stack — Minimal (Phase 6)

| Layer | Choice | Purpose | MVP? | Why not alt |
|---|---|---|---|---|
| FW/Lang/Build | React 18 + TS + Vite 5 | HMR, pool | YES | No Next.js/SSR, no Angular |
| Routing | React Router 6 | `RequireAuth`, `RequireRole` | YES | — |
| Server state | TanStack Query 5 | Cache, poll bell/messages | YES | Replaces Redux |
| Global | Zustand (auth only) | Tiny | YES | No Redux |
| API | Axios + 401-refresh | Headers, errors | YES | — |
| Forms | RHF + Zod | Proposal/session/checklist | YES | — |
| UI | Tailwind + shadcn-lite (Button/Input/Card/Table/Dialog/Badge) | Speed | YES | No MUI |
| Charts | Recharts | 2 dashboard charts | YES | ECharts heavier |
| Tables | Native + pagination | Lists | YES | No TanStack Table |
| Toast | Sonner | UX | YES | — |
| Test | Vitest + RTL + 1 Playwright | Happy path | STRETCH | Manual QA first |
| Deploy | Nginx `dist/` | Compose | YES | Vercel ok alt |

Do NOT add: Redux, Storybook, i18n, PWA, WS client, monorepo.

---

## 7. Component + Routing (Phases 7–8)

```text
src/
  app/router.tsx  api/client.ts  api/*.ts  types/dto.ts
  stores/auth.ts  hooks/useAuth.ts
  layouts/AppShell.tsx
  pages/Login.tsx Register.tsx Dashboard.tsx Sessions.tsx SessionDetail.tsx
        Tasks.tsx Projects.tsx ProjectDetail.tsx Proposals.tsx Leaderboards.tsx AdminAudit.tsx
  features/sessions/*  features/projects/WorkBoard.tsx  features/reco/RecoPanel.tsx
  components/ui/*  components/Table.tsx  components/Empty.tsx  components/Error.tsx
  validation/schemas.ts  utils/format.ts
```

| Route | Purpose | Role | APIs | Components |
|---|---|---|---|---|
| `/login`, `/register` | Auth | public | `POST /auth/*` | AuthForm, error banner |
| `/dashboard` | Student/Core stats | auth | `/analytics/me`, `/overview` | StatCards, SkillBars, Feed, RecoPanel |
| `/sessions`, `/sessions/:id` | List/detail/attendance/resources/tasks | auth; mutate Lead/Core | sessions, attendance, resources, tasks | FilterBar, SessionTable, MarkAttendance, TaskChecklist |
| `/projects`, `/projects/:id`, `/proposals/new` | List/detail/propose | auth; review Lead/Core | proposals, review, join, work-items, messages | ProposalForm, StatusBadge, JoinBtn, WorkBoard, MessageBox, Timeline |
| `/recommendations` | Project + learning | own | `/me/project-recommendations` | RecoCard + Why |
| `/leaderboards` | Overall + domain tabs | auth | `/leaderboards*` | Tabs, RankTable |
| `/notifications` | List + read | auth | `/notifications` | Bell poll 30s |
| `/admin/audit`, `/admin/users` | Audit + roles | CORE/SUPER | `/admin/*` | AuditTable, RoleSelect |

All pages: loading skeleton / error retry / empty state + permission-gated buttons.

---

## 8. API Contract — Frozen Day 1 (Phase 9–10)

Base `/api/v1` · `Authorization: Bearer <access>` · IDs `Long` · Time ISO-8601 UTC · `?page=0&size=10&sort=createdAt,desc&q=&domain=&status=`

```json
// error shape
{ "code": "PROJECT_NOT_FOUND", "message": "...", "fieldErrors": {"title":"required"}, "traceId": "...", "timestamp": "..." }
```

`200/201/204/400/401/403/404/409/422`. Validation → 400 + `fieldErrors`.

```text
POST /auth/register {name,email,password,program} → 201 {user}
POST /auth/login {email,password} → 200 {accessToken,refreshToken,user}
POST /auth/refresh {refreshToken} → 200 {...}
POST /auth/logout → 204

GET  /domains
GET  /sessions?domain=&type=&q=&page=
POST /sessions (Lead/Core) {topic,domainId,type,date,time,description,objectives[],instructor}
GET/PATCH/DELETE /sessions/{id}
POST /sessions/{id}/attendance {userId,status}
GET  /me/attendance
GET/POST /sessions/{id}/resources
GET  /sessions/{id}/resource-recommendations → [{resource,score,breakdown}]
POST /sessions/{id}/tasks {title,requirements[],deadline,verificationRequired,assigneeId?}
POST /tasks/{id}/complete {repoUrl,commitSha,notes} → 409 if required && !sha
POST /tasks/{id}/verify {verified} (Lead)

POST /proposals {name,domainId,problem,solution,technologies[],requiredSkills[],expectedMembers,outcome} → status=PROPOSED
POST /projects/{id}/review {decision:APPROVE|CHANGES_REQUESTED|REJECT,comment} → APPROVE=ACTIVE + creator OWNER
GET  /projects?domain=&status=ACTIVE&q=
GET  /projects/{id}
POST /projects/{id}/join → REQUESTED
POST /projects/{id}/members/{uid}/approve (Lead/Core)
GET/POST/PATCH /projects/{id}/work-items {title,assigneeId,priority,deadline,status}
GET  /projects/{id}/activity
GET/POST /projects/{id}/messages

GET /me/project-recommendations?limit=5 → [{projectId,name,matchPercent,why[]}]
GET /me/skills  GET /me/xp  GET /me/learning-recommendations
GET /leaderboards/overall?program=TECHNICAL  GET /leaderboards/domain/{domainId}
GET /notifications?unread=true  POST /notifications/{id}/read
GET /analytics/me  GET /analytics/overview (Core/Faculty)
GET /admin/audit  PATCH /admin/users/{id}/role
```

**Split:** all scoring/progress/XP/permissions in BE. FE renders `why[]`/`breakdown`, never computes. FE may mock `GET /sessions,/projects,/leaderboards` Day 1.

### Auth & Security (Phase 11)

Register(BCrypt-12) → Login → access 15m `JWT{sub,role,domains}` + refresh UUID hashed 7d → 401 → `/refresh` rotate → logout delete. FE: memory + localStorage refresh, Zustand user, guards. BE is boundary: filter chain + `AuthorizationService` per mutate. Reset MVP: Super/Core sets temp password. Audit `ROLE_MANAGE, PROJECT_APPROVE, TASK_VERIFY`.

---

## 9. Algorithms + Recommendation — Java Deterministic (Phases 12–13)

| Algo | I/O | Method / Complexity | MVP |
|---|---|---|---|
| Progress | work_items → % | `done/total*100` O(n) sync | S |
| XP total | events → sum | SQL `SUM GROUP BY` | S |
| Skill | attendance(1)+task(3)+work(5)+commit(2)/domain → 0-100 | Weighted + cap O(a) | S |
| Leaderboard | xp sums | `ORDER BY sum DESC LIMIT 50` + index, T.Query 60s | S |
| Achievement | event → unlock | 5 if-rules O(1): FirstTask, Builder, 10Tasks, Mentor, Specialist(skill>80) | S |
| Resource reco | prefs(topics,type,diff) vs resources | `0.4*jaccard +0.2*type +0.15*diff +0.15*domain +0.1*quality` O(R) | M |
| **Project reco** | context{topics,techs,skills,recentSessions,work} × active projects | `0.40*learnTopic +0.25*workOverlap +0.15*skillFit +0.10*tech +0.10*role` → Top-5 + `why[]`. Jaccard on tokens. O(P·F). | M |

Cold start: popular-in-program + newest with `why=["Popular for beginners"]`. Feedback `POST /recommendations/{id}/feedback`. Metric: CTR only. Future: cosine/collab/embeddings — post-MVP.

Learning rules (Strategy, 3 impls): `RedisGapRule`, `TestingGapRule`, `AuthGapRule` : `evaluate(SkillProfile) → Optional<Reco>`.

Example: `IF postgres>60 AND spring>40 AND redis<20 THEN Recommend Redis Fundamentals`

---

## 10. Database — Postgres Minimal (Phase 14)

```sql
users(id, name, email UNIQUE, password_hash, role, program, created_at)
domains(id, name UNIQUE) -- 5 seeds
user_domains(user_id, domain_id)
sessions(id, domain_id FK, topic, type, date, time, description, objectives TEXT[], instructor, lead_id FK, program, created_at)
attendance(id, session_id FK, user_id FK, status, created_at, UNIQUE(session_id,user_id))
resources(id, session_id FK NULL, title, url, type, difficulty, topics TEXT[], domain_id FK, quality INT)
tasks(id, session_id FK, title, requirements TEXT, deadline, verification_required BOOL, assignee_id FK NULL, status, created_at)
task_completions(id, task_id FK, user_id FK, repo_url, commit_sha, verified BOOL, verified_by FK NULL, created_at)
projects(id, name, domain_id FK, status, problem, solution, technologies TEXT[], required_skills TEXT[], expected_members INT, outcome, created_by FK, approved_by FK NULL, program, created_at)
project_members(id, project_id FK, user_id FK, role, status, UNIQUE(project_id,user_id))
work_items(id, project_id FK, title, assignee_id FK NULL, status, priority, deadline, created_at)
activities(id, project_id FK NULL, actor_id FK, kind, text, evidence_ref NULL, created_at)
messages(id, project_id FK, sender_id FK, content, created_at)
skills(user_id FK, domain_id FK, score INT, updated_at, PK(user,domain))
xp_events(id, user_id FK, amount INT, source, ref_id NULL, created_at)
achievements(id, code UNIQUE, name, rule) -- 5 seeds
user_achievements(user_id, achievement_id, earned_at, PK)
notifications(id, user_id FK, kind, title, body, link, read BOOL, created_at)
audit_logs(id, actor_id FK, action, resource, resource_id, result, meta JSONB, created_at)
refresh_tokens(id, user_id FK, token_hash UNIQUE, expires_at)
-- indexes: attendance_user, sessions_domain_date, projects_status_domain, work_project_status, xp_user, notif_user_read, audit_created
```

Future (not MVP): `learning_goals, resource_feedback, general messages, permission tables, blobs`.

XP table: `Session +5, Resource +10, Task +15, Mentoring +20, PR merged +30, Contribution +40, Project done +100`.

---

## 11. Spring Modular Monolith (Phases 15–16)

```text
com.kernel.members/
  config/{SecurityConfig,WebConfig,AsyncConfig,OpenApiConfig}
  common/{BaseEntity,ApiException,GlobalExceptionHandler,PageDto}
  auth/ member/ domain/
  session/ attendance/ resource/
  task/{TaskController,TaskService,GitHubVerifier}
  project/{ProjectController,ProjectService,WorkItemService,ActivityService}
  chat/ recommendation/scoring/*Strategy
  skill/ gamification/ notification/ audit/ analytics/
```

Rules: Controller → Service → Repository, DTO in/out, `@Transactional` multi-write, `Verifier` port (`RegexVerifier` now, `GitHubApiVerifier` later), Strategy for reco/achieve/XP, SOLID small services, no `KernelService` god class.

---

## 12. State / Perf / Async / Testing / Deploy (Phases 18–22)

**State:** Server=T.Query · Global=Zustand(auth only) · Local=useState · Form=RHF · URL=searchParams · Auth=Zustand+interceptor. No Redux.

**Perf (only):** pagination + projections + EntityGraph + B-tree + T.Query 30-60s + lazy Admin/Analytics + debounce 300ms. No Redis.

**Async:** Spring Events sync; `@Async` notify only; `@Scheduled` deadlines. Idempotency: UNIQUE(task,user), UNIQUE(project,user).

**Tests (protect demo):** `RecoScoringTest` (weights=100, doc 86/100 case), `ProgressCalcTest`, `AuthzTest` (Backend 200/Frontend 403), `ProjectLifecycleTest`, 1 MockMvc/controller, verifier mock. FE: Zod + guards + 1 Playwright `login→attend→task→reco`.

**Deploy:**

```yaml
postgres:16-alpine + backend (temurin:25-jre) + frontend (node build → nginx)
env: SPRING_DATASOURCE_URL, JWT_SECRET, GITHUB_TOKEN (blank MVP)
up: docker compose up --build → BE:8080 FE:3000 PG:5432 (Flyway seeds)
```

Future prod: managed PG + Render/Fly + Vercel. No K8s.

---

## 13. Final Checklist (Phase 24)

**MVP:** auth+scoped RBAC, sessions, attendance, curated resources+reco, tasks+mock-GitHub, proposal→approve→project, join, work-items+progress, activity, project chat, reco+Why+skills+XP+5 achieve+boards, notify poll, audit basic, 2 dashboards, paginate.

**Deferred:** live GitHub, WS, Redis/Kafka/ES/K8s, ML, crawler, permission UI, mail reset, reports, uploads.

**DONE =** `compose up` → login each role → mark attendance → complete task w/ SHA → propose → approve → join → update work-item → reco with Why → XP/board moves → audit row — zero 500s.

### Tech Table

| Layer | Technology | Purpose | MVP? | Reason |
|---|---|---|---|---|
| Backend | Java 25 + SB 3.3 Web/Security/JPA | REST+authz+ORM | YES | Skill + ecosystem |
| DB | Postgres 16 + Flyway | Truth + migrations | YES | Relational + ILIKE/JSONB |
| Auth | JWT + BCrypt | Stateless | YES | Simple demo |
| Reco | Pure Java weighted + Strategy | Top-5 explainable | YES | Deterministic, no ML |
| Events | Spring Events + Async/Scheduled | XP/notify decoupling | YES | No broker |
| Contract | Springdoc OpenAPI | Parallel FE | YES | Freeze |
| Frontend | React18+TS+Vite+Router+T.Query+Zustand+Axios+RHF+Zod+Tailwind+Recharts+Sonner | Fast demo | YES | Smallest productive set |
| Testing | JUnit5/MockMvc + Vitest/Playwright(1) | Guard paths | Lite | Algo+authz only |
| DevOps | Docker Compose | One-cmd demo | YES | No K8s |
| Cache/Search/RT | T.Query + SQL ILIKE + poll | Good enough | NO | Redis/ES/WS deferred |
| Monitor | Actuator + JSON logs | Health | Lite | No Prometheus |

**Next:** leads sign §8 table Day 1 10am, then parallel Day 1 tasks.
