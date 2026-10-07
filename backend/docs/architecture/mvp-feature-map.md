# MIT TECH KERNEL Backend - MVP Feature Map

This document provides the definitive mapping of every MVP feature to its owning module, domain concepts, dependencies, and persistence requirements.

---

## Module Catalog

| Module | Type | Status |
|--------|------|--------|
| auth | Core | Existing |
| user | Core | Existing |
| member | Core | Existing |
| domain | Core | Existing |
| session | Core | Renamed from `event` |
| registration | Core | Existing (session registration) |
| resource | Core | **New** |
| task | Core | **New** |
| project | Core | **New** |
| skill | Core | **New** |
| gamification | Core | **New** |
| notification | Core | **New** |
| recommendation | Core | **New** |
| foundation | Core | Existing |
| leaderboard | Core | Existing |
| analytics | Supporting | **New** |
| audit | Supporting | **New** |

---

## Feature → Module Mapping

### 1. AUTHENTICATION & ACCESS CONTROL

| Aspect | Details |
|--------|---------|
| **Module** | `auth` |
| **Responsibility** | Authentication, JWT token management, password hashing, session management |
| **Domain Concepts** | UserCredentials, RefreshToken, AccessToken, PasswordResetToken, AuthenticationAttempt, MfaConfiguration |
| **Dependencies** | `user` (read user identity), `common` (exceptions, response, util) |
| **Core/Supporting** | Core |
| **Major APIs** | POST `/api/v1/auth/login`, POST `/api/v1/auth/refresh`, POST `/api/v1/auth/logout`, POST `/api/v1/auth/password/reset`, POST `/api/v1/auth/password/change`, POST `/api/v1/auth/mfa/*` |
| **Persistence** | `user_credentials`, `refresh_tokens`, `password_reset_tokens`, `authentication_attempts`, `mfa_configurations` |

---

### 2. USER & MEMBER MANAGEMENT

| Aspect | Details |
|--------|---------|
| **Module** | `user` (identity), `member` (membership) |
| **Responsibility** | User profiles, member profiles, skills, domain associations, member search/filtering, pagination, authorized member management |
| **Domain Concepts (user)** | User, UserProfile, RoleAssignment, DomainLeadAssignment |
| **Domain Concepts (member)** | Member, MemberApplication, MembershipPeriod, MemberSkill, MemberDomainInterest |
| **Dependencies** | `user` → `common`, `auth` (read); `member` → `user` (query), `domain` (query), `common` |
| **Core/Supporting** | Core |
| **Major APIs (user)** | GET/POST `/api/v1/users`, GET/PUT `/api/v1/users/{id}`, GET/PUT `/api/v1/users/{id}/profile`, GET/POST `/api/v1/users/{id}/roles`, GET/POST `/api/v1/users/{id}/domain-leads` |
| **Major APIs (member)** | GET/POST `/api/v1/members`, GET/PUT `/api/v1/members/{id}`, GET/POST `/api/v1/members/applications`, GET/PUT `/api/v1/members/applications/{id}`, GET `/api/v1/members?search=&domain=&skill=&page=` |
| **Persistence** | `users`, `user_profiles`, `user_roles`, `domain_lead_assignments`, `members`, `member_applications`, `membership_periods`, `member_skills`, `member_domain_interests` |

---

### 3. TECHNICAL DOMAINS

| Aspect | Details |
|--------|---------|
| **Module** | `domain` |
| **Responsibility** | Five fixed domains, domain information, domain leads, domain-scoped management, domain-specific activities, domain-specific leaderboards |
| **Domain Concepts** | Domain, DomainLead, DomainMember, DomainSettings, DomainActivity |
| **Fixed Domains** | General, Frontend, Backend, AI/ML/IoT, Competitive Programming |
| **Dependencies** | `user` (query leads/members), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/domains`, GET/PUT `/api/v1/domains/{id}`, GET/POST `/api/v1/domains/{id}/leads`, GET/POST `/api/v1/domains/{id}/members`, GET `/api/v1/domains/{id}/activities`, GET `/api/v1/domains/{id}/leaderboard` |
| **Persistence** | `domains`, `domain_leads`, `domain_members`, `domain_settings`, `domain_activities` |

---

### 4. SESSIONS

| Aspect | Details |
|--------|---------|
| **Module** | `session` (renamed from `event`) |
| **Responsibility** | Create/manage sessions, session scheduling, domain association, session participation, session history |
| **Domain Concepts** | Session, SessionSchedule, SessionDomain, SessionParticipant, SessionHistory, SessionConfiguration |
| **Dependencies** | `domain` (query), `user` (query), `member` (query), `resource` (query), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/sessions`, GET/PUT `/api/v1/sessions/{id}`, GET/POST `/api/v1/sessions/{id}/schedule`, GET/POST `/api/v1/sessions/{id}/participants`, GET `/api/v1/sessions/{id}/history`, GET `/api/v1/sessions?domain=&date=&status=` |
| **Persistence** | `sessions`, `session_schedules`, `session_domains`, `session_participants`, `session_histories`, `session_configurations` |

---

### 5. ATTENDANCE

| Aspect | Details |
|--------|---------|
| **Module** | `session` (owned by session module) |
| **Responsibility** | Mark attendance, session-wise attendance, attendance history, attendance analytics |
| **Domain Concepts** | AttendanceRecord, AttendanceSession, AttendanceStatus, AttendanceAnalytics |
| **Dependencies** | `session` (owns), `registration` (query registrations), `user` (query), `analytics` (publish events) |
| **Core/Supporting** | Core |
| **Major APIs** | POST `/api/v1/sessions/{id}/attendance`, GET `/api/v1/sessions/{id}/attendance`, GET `/api/v1/users/{id}/attendance`, GET `/api/v1/analytics/attendance` |
| **Persistence** | `attendance_records`, `attendance_sessions`, `attendance_analytics` |

---

### 6. RESOURCES

| Aspect | Details |
|--------|---------|
| **Module** | `resource` **(New Module)** |
| **Responsibility** | Curated resource library organized by domain, topic, difficulty, resource type; optional session/topic association |
| **Domain Concepts** | Resource, ResourceDomain, ResourceTopic, ResourceDifficulty, ResourceType, ResourceSessionLink |
| **Resource Types** | Documentation, Course, Tutorial, Video, Article, Practice Platform, GitHub Repository |
| **Dependencies** | `domain` (query), `session` (query, optional), `user` (query for curator), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/resources`, GET/PUT `/api/v1/resources/{id}`, GET `/api/v1/resources?domain=&topic=&difficulty=&type=`, GET `/api/v1/resources/{id}/sessions`, POST `/api/v1/resources/{id}/sessions` |
| **Persistence** | `resources`, `resource_domains`, `resource_topics`, `resource_difficulties`, `resource_types`, `resource_session_links` |

---

### 7. POST-SESSION TASKS

| Aspect | Details |
|--------|---------|
| **Module** | `task` **(New Module)** |
| **Responsibility** | Create tasks, assign tasks, track task status, track completion |
| **Domain Concepts** | Task, TaskAssignment, TaskStatus, TaskSubmission, TaskType, TaskDeadline |
| **Dependencies** | `session` (query), `project` (query), `user` (query), `skill` (query), `gamification` (publish completion events), `notification` (publish assignment events), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/tasks`, GET/PUT `/api/v1/tasks/{id}`, POST `/api/v1/tasks/{id}/assign`, PUT `/api/v1/tasks/{id}/status`, POST `/api/v1/tasks/{id}/submit`, GET `/api/v1/tasks/{id}/submissions` |
| **Persistence** | `tasks`, `task_assignments`, `task_statuses`, `task_submissions`, `task_types`, `task_deadlines` |

---

### 8. GITHUB-VERIFIED COMPLETION

| Aspect | Details |
|--------|---------|
| **Module** | `task` (part of task module) |
| **Responsibility** | GitHub repository URL/branch/commit SHA evidence, format validation, evidence submission, Domain Lead verification |
| **Domain Concepts** | GitHubEvidence, EvidenceFormat, EvidenceSubmission, EvidenceVerification, VerificationStatus |
| **Dependencies** | `task` (owns), `domain` (query leads for verification), `user` (query), `notification` (publish verification events) |
| **Core/Supporting** | Core |
| **Major APIs** | POST `/api/v1/tasks/{id}/evidence`, PUT `/api/v1/tasks/{id}/evidence`, POST `/api/v1/tasks/{id}/evidence/verify`, GET `/api/v1/tasks/{id}/evidence` |
| **Persistence** | `github_evidence`, `evidence_submissions`, `evidence_verifications` |

---

### 9. PROJECTS

| Aspect | Details |
|--------|---------|
| **Module** | `project` **(New Module)** |
| **Responsibility** | Project lifecycle: Proposal → Approval → Join → Work → Evidence/Activity → Completion |
| **Domain Concepts** | Project, ProjectProposal, ProjectApproval, ProjectMember, ProjectWorkItem, ProjectEvidence, ProjectStatus, ProjectMatching |
| **Dependencies** | `user` (query), `member` (query), `domain` (query), `task` (query/create work items), `skill` (query), `gamification` (publish completion), `notification` (publish events), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/projects`, GET/PUT `/api/v1/projects/{id}`, POST `/api/v1/projects/{id}/propose`, POST `/api/v1/projects/{id}/approve`, POST `/api/v1/projects/{id}/join`, GET/POST `/api/v1/projects/{id}/work-items`, GET/POST `/api/v1/projects/{id}/evidence`, GET `/api/v1/projects?status=&domain=` |
| **Persistence** | `projects`, `project_proposals`, `project_approvals`, `project_members`, `project_work_items`, `project_evidence`, `project_statuses`, `project_matchings` |

---

### 10. PROJECT DISCUSSIONS

| Aspect | Details |
|--------|---------|
| **Module** | `project` (part of project module) |
| **Responsibility** | Project discussions, project communication, REST polling for MVP |
| **Domain Concepts** | Discussion, DiscussionThread, DiscussionMessage, DiscussionParticipant |
| **Dependencies** | `project` (owns), `user` (query), `notification` (publish events) |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/projects/{id}/discussions`, GET/POST `/api/v1/projects/{id}/discussions/{threadId}/messages`, GET `/api/v1/projects/{id}/discussions?polling=` |
| **Persistence** | `project_discussions`, `discussion_threads`, `discussion_messages`, `discussion_participants` |

---

### 11. SKILLS & SKILL SCORING

| Aspect | Details |
|--------|---------|
| **Module** | `skill` **(New Module)** |
| **Responsibility** | Track member skills, skill-related activity, skill scoring, domain/technical skill progression |
| **Domain Concepts** | Skill, SkillCategory, MemberSkill, SkillScore, SkillProgression, SkillActivity |
| **Dependencies** | `member` (query), `domain` (query), `task` (query completions), `project` (query completions), `session` (query attendance), `gamification` (publish skill XP events), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/skills`, GET/PUT `/api/v1/skills/{id}`, GET/POST `/api/v1/members/{id}/skills`, GET `/api/v1/members/{id}/skills/{skillId}/progression`, GET `/api/v1/skills?domain=&category=` |
| **Persistence** | `skills`, `skill_categories`, `member_skills`, `skill_scores`, `skill_progressions`, `skill_activities` |

---

### 12. PERSONALIZED RECOMMENDATIONS

| Aspect | Details |
|--------|---------|
| **Module** | `recommendation` **(New Module)** |
| **Responsibility** | Personalized recommendations for projects, tasks, learning opportunities/activities; deterministic/weighted scoring; explainable "why" |
| **Domain Concepts** | Recommendation, RecommendationType, RecommendationScore, RecommendationReason, RecommendationContext |
| **Recommendation Types** | Project, Task, Learning Opportunity, Activity |
| **Dependencies** | `project` (query), `task` (query), `session` (query), `resource` (query), `skill` (query), `member` (query), `user` (query), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET `/api/v1/recommendations`, GET `/api/v1/recommendations?type=`, GET `/api/v1/recommendations/{id}/explanation` |
| **Persistence** | `recommendations`, `recommendation_scores`, `recommendation_reasons`, `recommendation_contexts` |

---

### 13. XP & ACHIEVEMENTS

| Aspect | Details |
|--------|---------|
| **Module** | `gamification` **(New Module)** |
| **Responsibility** | XP system, XP from relevant activities, achievement tracking, five hardcoded MVP achievements |
| **Domain Concepts** | XpLedger, XpEvent, Achievement, AchievementDefinition, MemberAchievement, AchievementTier |
| **Fixed Achievements** | 1. First Session Attended, 2. First Task Completed, 3. First Project Completed, 4. Domain Explorer (all 5 domains), 5. Foundation Graduate |
| **Dependencies** | `session` (events), `task` (events), `project` (events), `skill` (events), `foundation` (events), `domain` (query), `user` (query), `leaderboard` (publish XP), `notification` (publish achievement events), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET `/api/v1/gamification/xp/{userId}`, GET `/api/v1/gamification/achievements/{userId}`, POST `/api/v1/gamification/xp/events` (internal), GET `/api/v1/gamification/achievements/definitions` |
| **Persistence** | `xp_ledger`, `xp_events`, `achievements`, `achievement_definitions`, `member_achievements` |

---

### 14. LEADERBOARDS

| Aspect | Details |
|--------|---------|
| **Module** | `leaderboard` |
| **Responsibility** | Overall technical leaderboard, domain-specific technical leaderboards, XP/scoring-based ranking |
| **Domain Concepts** | Leaderboard, LeaderboardEntry, LeaderboardType, LeaderboardPeriod, RankingSnapshot, ScoringRule |
| **Dependencies** | `gamification` (query XP), `domain` (query), `user` (query), `member` (query), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET `/api/v1/leaderboards`, GET `/api/v1/leaderboards/{id}`, GET `/api/v1/leaderboards/{id}/entries`, GET `/api/v1/leaderboards/domain/{domainId}`, GET `/api/v1/leaderboards/overall` |
| **Persistence** | `leaderboards`, `leaderboard_entries`, `leaderboard_types`, `leaderboard_periods`, `ranking_snapshots`, `scoring_rules` |

---

### 15. FOUNDATION PROGRAM

| Aspect | Details |
|--------|---------|
| **Module** | `foundation` |
| **Responsibility** | Foundation participants, activities, progress, tracking, Foundation-specific leaderboard (separate from technical) |
| **Domain Concepts** | FoundationProgram, FoundationCohort, FoundationParticipant, FoundationRole (PARTICIPANT, MENTOR, COORDINATOR), FoundationActivity, FoundationProgress, FoundationLeaderboard |
| **Dependencies** | `user` (query), `member` (query), `domain` (query), `task` (query), `session` (query), `gamification` (separate XP?), `leaderboard` (separate foundation leaderboard), `notification`, `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET/POST `/api/v1/foundation/programs`, GET/PUT `/api/v1/foundation/programs/{id}`, GET/POST `/api/v1/foundation/programs/{id}/enrollments`, GET/POST `/api/v1/foundation/programs/{id}/activities`, GET `/api/v1/foundation/programs/{id}/progress`, GET `/api/v1/foundation/leaderboard` |
| **Persistence** | `foundation_programs`, `foundation_cohorts`, `foundation_enrollments`, `foundation_roles`, `foundation_activities`, `foundation_progress`, `foundation_leaderboard` |

---

### 16. NOTIFICATIONS

| Aspect | Details |
|--------|---------|
| **Module** | `notification` **(New Module)** |
| **Responsibility** | User notifications, activity notifications, project notifications, task notifications, system notifications |
| **Domain Concepts** | Notification, NotificationTemplate, NotificationChannel, NotificationPreference, NotificationStatus, NotificationRecipient |
| **Dependencies** | `user` (query), `member` (query), `common` |
| **Core/Supporting** | Core |
| **Major APIs** | GET `/api/v1/notifications`, PUT `/api/v1/notifications/{id}/read`, PUT `/api/v1/notifications/read-all`, GET/PUT `/api/v1/notifications/preferences`, POST `/api/v1/notifications` (internal) |
| **Persistence** | `notifications`, `notification_templates`, `notification_channels`, `notification_preferences`, `notification_recipients` |

---

### 17. ANALYTICS

| Aspect | Details |
|--------|---------|
| **Module** | `analytics` **(New Module)** |
| **Responsibility** | Attendance analytics, participation analytics, task completion analytics, project activity analytics, member progress analytics, domain activity analytics, leaderboard-related analytics |
| **Domain Concepts** | AnalyticsReport, AnalyticsMetric, AnalyticsDimension, AnalyticsFilter, AnalyticsDashboard, AnalyticsSnapshot |
| **Dependencies** | `session` (events), `registration` (events), `task` (events), `project` (events), `member` (query), `domain` (query), `leaderboard` (query), `foundation` (query), `gamification` (query), `common` |
| **Core/Supporting** | Supporting |
| **Major APIs** | GET `/api/v1/analytics/attendance`, GET `/api/v1/analytics/participation`, GET `/api/v1/analytics/tasks`, GET `/api/v1/analytics/projects`, GET `/api/v1/analytics/members`, GET `/api/v1/analytics/domains`, GET `/api/v1/analytics/leaderboard`, GET `/api/v1/analytics/dashboard` |
| **Persistence** | `analytics_reports`, `analytics_metrics`, `analytics_snapshots`, `analytics_dashboards` (materialized views / aggregated tables) |

---

### 18. AUDIT LOGS

| Aspect | Details |
|--------|---------|
| **Module** | `audit` **(New Module)** |
| **Responsibility** | Track important system actions: actor, action, timestamp, affected resource, relevant metadata |
| **Domain Concepts** | AuditLog, AuditActor, AuditAction, AuditResource, AuditMetadata, AuditSeverity |
| **Dependencies** | All modules (publish events), `common` |
| **Core/Supporting** | Supporting |
| **Major APIs** | GET `/api/v1/audit/logs`, GET `/api/v1/audit/logs?actor=&action=&resource=&date=`, POST `/api/v1/audit/logs` (internal) |
| **Persistence** | `audit_logs` (append-only, partitioned by date) |

---

### 19. SEARCH, FILTERING & PAGINATION

| Aspect | Details |
|--------|---------|
| **Module** | Cross-cutting (implemented in each module) |
| **Responsibility** | Apply to members, projects, sessions, resources, leaderboards, other list-based resources |
| **Implementation** | Standard query parameters: `page`, `size`, `sort`, `search`, domain-specific filters |
| **Dependencies** | `common` (util for pagination helpers) |
| **Core/Supporting** | Cross-cutting |

---

## Cross-Module Dependency Matrix

| Consumer → Provider | auth | user | member | domain | session | registration | resource | task | project | skill | gamification | notification | recommendation | foundation | leaderboard | analytics | audit |
|---------------------|------|------|--------|--------|---------|--------------|----------|------|---------|-------|--------------|--------------|----------------|------------|-------------|-----------|-------|
| **auth** | - | R | - | - | - | - | - | - | - | - | - | - | - | - | - | - | W |
| **user** | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | W |
| **member** | - | R | - | R | - | - | - | - | - | - | - | - | - | - | - | - | W |
| **domain** | - | R | - | - | - | - | - | - | - | - | - | - | - | - | - | - | W |
| **session** | - | R | R | R | - | - | R | - | - | - | - | W | - | - | - | - | W |
| **registration** | - | R | - | - | R | - | - | - | - | - | - | W | - | - | - | - | W |
| **resource** | - | R | - | R | R | - | - | - | - | - | - | - | - | - | - | - | W |
| **task** | - | R | - | - | R | - | - | - | R | R | W | W | - | - | - | - | W |
| **project** | - | R | R | R | - | - | - | R | - | R | W | W | - | - | - | - | W |
| **skill** | - | - | R | R | R | - | - | R | R | - | W | - | - | - | - | - | W |
| **gamification** | - | R | - | R | W | - | - | W | W | W | - | W | - | W | W | - | W |
| **notification** | - | R | R | - | - | - | - | - | - | - | - | - | - | - | - | - | W |
| **recommendation** | - | R | R | - | R | - | R | R | R | R | - | - | - | - | - | - | W |
| **foundation** | - | R | R | R | R | - | - | R | - | - | W | W | - | - | W | - | W |
| **leaderboard** | - | R | R | R | - | - | - | - | - | - | R | - | - | R | - | - | W |
| **analytics** | - | R | R | R | R | R | R | R | R | R | R | - | - | R | R | - | W |
| **audit** | W | W | W | W | W | W | W | W | W | W | W | W | W | W | W | W | - |

**Legend:**
- `R` = Reads (via QueryService interface)
- `W` = Writes (publishes domain events / audit entries)
- `-` = No direct dependency

---

## New Modules Justification

| Module | Why New? |
|--------|----------|
| `resource` | First-class MVP feature; distinct bounded context (curated library); different lifecycle from sessions/tasks |
| `task` | Post-session tasks + project tasks + GitHub verification share core concepts (assignment, submission, status); distinct from sessions and projects |
| `project` | Full project lifecycle is a major bounded context; discussions, work items, evidence, matching |
| `skill` | Skill tracking/scoring is distinct from member profiles; feeds into gamification and recommendations |
| `gamification` | XP/achievements are cross-cutting but have own domain logic (ledger, definitions, tiers); not just leaderboard data |
| `notification` | Notification delivery, templates, preferences are a distinct capability; multiple modules publish to it |
| `recommendation` | Recommendation engine with explainable scoring is a distinct read-model service |
| `analytics` | Aggregated read models across domains; materialized views; separate from transactional modules |
| `audit` | Append-only audit trail; cross-cutting; all modules write to it via events |

---

## Module Renames

| Old Name | New Name | Reason |
|----------|----------|--------|
| `event` | `session` | MVP uses "session" terminology; sessions have scheduling, attendance, participation |

---

## Notes

- All cross-module reads happen via **QueryService interfaces** (see `dependency-rules.md`)
- All cross-module writes happen via **domain events** (published to `audit`, `gamification`, `notification`, `analytics`)
- No module directly accesses another module's JPA entities or repositories
- `session` module owns attendance (not a separate module)
- `task` module owns GitHub-verified completion (not separate)
- `project` module owns discussions (not separate)
- `recommendation` is a read-only aggregation service
- `analytics` and `audit` are supporting modules that aggregate events