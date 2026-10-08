-- V10: Day-3 demo dataset - powers the section 13 DONE walkthrough:
-- login each role -> mark attendance -> complete task w/ SHA -> propose -> approve
-- -> join -> update work-item -> reco with Why -> XP/board moves -> audit row.
-- NOTE: lookups (domains/users/sessions/projects/tasks) are resolved by natural
-- key subqueries/joins so the seed does not depend on generated IDs.

-- ---------- SESSIONS (10) ----------
INSERT INTO sessions (domain_id, program, topic, type, date, time,
                      description, objectives, instructor, lead_id)
SELECT d.id, v.program, v.topic, v.type, v.date, v.time,
       v.description, v.objectives, v.instructor, l.id
FROM (VALUES
    ('General', 'TECHNICAL', 'Club Kickoff', 'SOCIAL',
     DATE '2026-09-10', TIME '17:00', 'Welcome to the new Kernel term',
     ARRAY['Introduce domains', 'Announce roadmap'], 'Aarav Sharma', NULL::text),
    ('Backend', 'TECHNICAL', 'REST APIs with Spring Boot', 'WORKSHOP',
     DATE '2026-09-15', TIME '17:00', 'Build and secure a REST API end to end',
     ARRAY['Design endpoints', 'Apply validation', 'Secure with JWT'],
     'Prem Kumar', 'lead.backend@kernel.ac.in'),
    ('Frontend', 'TECHNICAL', 'React State Management', 'WORKSHOP',
     DATE '2026-09-18', TIME '16:00', 'Server state vs client state in React apps',
     ARRAY['Model server state', 'Cache with TanStack Query', 'Avoid prop drilling'],
     'Sara Khan', 'lead.frontend@kernel.ac.in'),
    ('Backend', 'TECHNICAL', 'PostgreSQL Fundamentals', 'WORKSHOP',
     DATE '2026-09-22', TIME '17:00', 'Relational modelling and query tuning',
     ARRAY['Normalise a schema', 'Write JOINs', 'Read an EXPLAIN plan'],
     'Prem Kumar', 'lead.backend@kernel.ac.in'),
    ('AI/ML/IoT', 'TECHNICAL', 'Intro to Machine Learning', 'TALK',
     DATE '2026-09-25', TIME '18:00', 'What ML is and where it fits',
     ARRAY['Supervised vs unsupervised', 'Train/test split'], 'Guest Speaker',
     'lead.backend@kernel.ac.in'),
    ('Competitive Programming', 'TECHNICAL', 'Algorithmic Problem Solving', 'WORKSHOP',
     DATE '2026-09-28', TIME '16:00', 'Core patterns for contest problems',
     ARRAY['Two pointers', 'Graph traversal', 'Binary search'], 'Diya Patel', NULL::text),
    ('General', 'TECHNICAL', 'Open Source Contribution Day', 'HACKATHON',
     DATE '2026-10-03', TIME '10:00', 'First PRs to open source projects',
     ARRAY['Find an issue', 'Open a pull request'], 'Diya Patel', NULL::text),
    ('Frontend', 'TECHNICAL', 'Accessible UI Patterns', 'TALK',
     DATE '2026-10-13', TIME '16:00', 'Building interfaces everyone can use',
     ARRAY['Keyboard navigation', 'ARIA basics'], 'Sara Khan', 'lead.frontend@kernel.ac.in'),
    ('AI/ML/IoT', 'TECHNICAL', 'Data Cleaning with Python', 'WORKSHOP',
     DATE '2026-10-15', TIME '17:30', 'Pandas workflows for messy data',
     ARRAY['Handle nulls', 'Group and aggregate'], 'Prem Kumar', 'lead.backend@kernel.ac.in'),
    ('Competitive Programming', 'TECHNICAL', 'Mock Contest', 'HACKATHON',
     DATE '2026-10-17', TIME '10:00', 'Timed practice contest',
     ARRAY['Solve 5 problems', 'Review solutions'], 'Diya Patel', NULL::text)
) AS v(domain_name, program, topic, type, date, time, description, objectives,
       instructor, lead_email)
JOIN domains d ON d.name = v.domain_name
LEFT JOIN users l ON l.email = v.lead_email;

-- ---------- ATTENDANCE (11) ----------
INSERT INTO attendance (session_id, user_id, status, marked_by)
SELECT s.id, u.id, v.status, m.id
FROM (VALUES
    ('REST APIs with Spring Boot', 'student@kernel.ac.in', 'PRESENT', 'lead.backend@kernel.ac.in'),
    ('React State Management', 'student@kernel.ac.in', 'PRESENT', 'lead.frontend@kernel.ac.in'),
    ('PostgreSQL Fundamentals', 'student@kernel.ac.in', 'PRESENT', 'lead.backend@kernel.ac.in'),
    ('Intro to Machine Learning', 'student@kernel.ac.in', 'LATE', 'lead.backend@kernel.ac.in'),
    ('Algorithmic Problem Solving', 'student@kernel.ac.in', 'PRESENT', 'core@kernel.ac.in'),
    ('Open Source Contribution Day', 'student@kernel.ac.in', 'PRESENT', 'core@kernel.ac.in'),
    ('Club Kickoff', 'student@kernel.ac.in', 'PRESENT', 'superadmin@kernel.ac.in'),
    ('Club Kickoff', 'lead.frontend@kernel.ac.in', 'PRESENT', 'superadmin@kernel.ac.in'),
    ('Club Kickoff', 'core@kernel.ac.in', 'PRESENT', 'superadmin@kernel.ac.in'),
    ('Open Source Contribution Day', 'core@kernel.ac.in', 'PRESENT', 'superadmin@kernel.ac.in'),
    ('Open Source Contribution Day', 'lead.backend@kernel.ac.in', 'PRESENT', 'core@kernel.ac.in')
) AS v(topic, email, status, marker_email)
JOIN sessions s ON s.topic = v.topic
JOIN users u ON u.email = v.email
JOIN users m ON m.email = v.marker_email;

-- ---------- RESOURCES (9, curated library for reco V1) ----------
INSERT INTO resources (session_id, domain_id, title, url, type, difficulty,
                       topics, quality, created_by)
SELECT s.id, d.id, v.title, v.url, v.type, v.difficulty, v.topics, v.quality, c.id
FROM (VALUES
    ('REST APIs with Spring Boot', 'Backend', 'Spring Boot Reference Guide',
     'https://spring.io/guides/gs-rest-service', 'DOCUMENTATION', 'BEGINNER',
     ARRAY['spring', 'rest', 'java'], 5, 'lead.backend@kernel.ac.in'),
    ('REST APIs with Spring Boot', 'Backend', 'Building REST APIs Course',
     'https://example.org/rest-apis-course', 'COURSE', 'INTERMEDIATE',
     ARRAY['rest', 'api', 'spring'], 4, 'lead.backend@kernel.ac.in'),
    ('PostgreSQL Fundamentals', 'Backend', 'PostgreSQL JOINs Explained',
     'https://example.org/postgres-joins', 'VIDEO', 'BEGINNER',
     ARRAY['postgres', 'sql'], 4, 'lead.backend@kernel.ac.in'),
    ('PostgreSQL Fundamentals', 'Backend', 'SQL Practice Platform',
     'https://example.org/sql-practice', 'PRACTICE_PLATFORM', 'INTERMEDIATE',
     ARRAY['sql', 'postgres'], 5, 'lead.backend@kernel.ac.in'),
    ('React State Management', 'Frontend', 'React Query Essentials',
     'https://example.org/react-query', 'TUTORIAL', 'INTERMEDIATE',
     ARRAY['react', 'state', 'query'], 4, 'lead.frontend@kernel.ac.in'),
    ('React State Management', 'Frontend', 'React Official Docs',
     'https://react.dev', 'DOCUMENTATION', 'BEGINNER',
     ARRAY['react', 'hooks'], 5, 'lead.frontend@kernel.ac.in'),
    ('Intro to Machine Learning', 'AI/ML/IoT', 'Kaggle Intro to Machine Learning',
     'https://example.org/kaggle-ml', 'COURSE', 'BEGINNER',
     ARRAY['ml', 'python'], 5, 'lead.backend@kernel.ac.in'),
    ('Algorithmic Problem Solving', 'Competitive Programming', 'LeetCode Patterns',
     'https://example.org/leetcode-patterns', 'PRACTICE_PLATFORM', 'INTERMEDIATE',
     ARRAY['algorithms', 'graphs'], 4, 'core@kernel.ac.in')
) AS v(session_topic, domain_name, title, url, type, difficulty, topics, quality, curator_email)
JOIN sessions s ON s.topic = v.session_topic
JOIN domains d ON d.name = v.domain_name
JOIN users c ON c.email = v.curator_email;

-- library resource without a session (feeds the learning reco rule:
-- pg>60 AND spring>40 AND redis<20 THEN Recommend Redis Fundamentals)
INSERT INTO resources (session_id, domain_id, title, url, type, difficulty,
                       topics, quality, created_by)
SELECT NULL, d.id, 'Redis Fundamentals', 'https://example.org/redis-fundamentals',
       'DOCUMENTATION', 'INTERMEDIATE', ARRAY['redis', 'cache'], 4, u.id
FROM domains d, users u
WHERE d.name = 'Backend' AND u.email = 'lead.backend@kernel.ac.in';

-- ---------- TASKS (8) ----------
INSERT INTO tasks (session_id, title, requirements, deadline,
                   verification_required, assignee_id, status)
SELECT s.id, v.title, v.requirements, v.deadline, v.verification_required, a.id, v.status
FROM (VALUES
    ('REST APIs with Spring Boot', 'Build a CRUD API',
     ARRAY['Model the entity', 'Implement CRUD endpoints', 'Validate input'],
     TIMESTAMP '2026-09-20 23:59', TRUE, 'student@kernel.ac.in', 'VERIFIED'),
    ('REST APIs with Spring Boot', 'Add JWT security',
     ARRAY['Issue access and refresh tokens', 'Protect all mutating routes'],
     TIMESTAMP '2026-09-22 23:59', TRUE, 'student@kernel.ac.in', 'OPEN'),
    ('PostgreSQL Fundamentals', 'Design a normalized schema',
     ARRAY['Third normal form', 'Document the keys'],
     TIMESTAMP '2026-09-27 23:59', FALSE, 'student@kernel.ac.in', 'COMPLETED'),
    ('PostgreSQL Fundamentals', 'Write five JOIN queries',
     ARRAY['Inner, left and full joins', 'Show EXPLAIN output'],
     TIMESTAMP '2026-09-29 23:59', FALSE, 'student@kernel.ac.in', 'OPEN'),
    ('React State Management', 'Implement a data-fetching hook',
     ARRAY['Wrap TanStack Query', 'Handle loading and errors'],
     TIMESTAMP '2026-09-25 23:59', TRUE, 'student@kernel.ac.in', 'OPEN'),
    ('Algorithmic Problem Solving', 'Solve ten graph problems',
     ARRAY['BFS and DFS', 'Shortest path', 'Union find'],
     TIMESTAMP '2026-10-05 23:59', TRUE, 'student@kernel.ac.in', 'VERIFIED'),
    ('Intro to Machine Learning', 'Train a binary classifier',
     ARRAY['Clean the data', 'Report accuracy and F1'],
     TIMESTAMP '2026-10-20 23:59', TRUE, NULL::text, 'OPEN'),
    ('Open Source Contribution Day', 'Raise a docs pull request',
     ARRAY['Pick a good first issue', 'Open a pull request'],
     TIMESTAMP '2026-10-10 23:59', TRUE, 'student@kernel.ac.in', 'OPEN')
) AS v(session_topic, title, requirements, deadline, verification_required,
       assignee_email, status)
JOIN sessions s ON s.topic = v.session_topic
LEFT JOIN users a ON a.email = v.assignee_email;

-- ---------- TASK COMPLETIONS (3) ----------
INSERT INTO task_completions (task_id, user_id, repo_url, commit_sha, notes,
                              verified, verified_by, verified_at)
SELECT t.id, u.id, v.repo_url, v.commit_sha, v.notes, v.verified, vb.id, v.verified_at
FROM (VALUES
    ('Build a CRUD API', 'student@kernel.ac.in',
     'https://github.com/alex-thomas/campus-api',
     'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', 'CRUD endpoints with validation',
     TRUE, 'lead.backend@kernel.ac.in', TIMESTAMP '2026-09-21 10:00'),
    ('Design a normalized schema', 'student@kernel.ac.in',
     'https://github.com/alex-thomas/pg-schema', NULL::text, 'Schema and ER diagram',
     FALSE, NULL::text, NULL::timestamp),
    ('Solve ten graph problems', 'student@kernel.ac.in',
     'https://github.com/alex-thomas/cp-graphs',
     '9f8e7d6c5b4a9f8e7d6c5b4a9f8e7d6c5b4a9f8e', 'All ten solutions in Go',
     TRUE, 'core@kernel.ac.in', TIMESTAMP '2026-10-06 18:00')
) AS v(task_title, email, repo_url, commit_sha, notes, verified,
       verifier_email, verified_at)
JOIN tasks t ON t.title = v.task_title
JOIN users u ON u.email = v.email
LEFT JOIN users vb ON vb.email = v.verifier_email;

-- ---------- PROJECTS (6, one per lifecycle state) ----------
INSERT INTO projects (name, domain_id, status, problem, solution, technologies,
                      required_skills, expected_members, outcome,
                      created_by, approved_by, review_comment)
SELECT v.name, d.id, v.status, v.problem, v.solution, v.technologies,
       v.required_skills, v.expected_members, v.outcome, c.id, ap.id, v.review_comment
FROM (VALUES
    ('Campus Feed API', 'Backend', 'ACTIVE',
     'No single place for club announcements and session recaps',
     'A typed REST feed with domains, pagination and reactions',
     ARRAY['Java', 'Spring Boot', 'PostgreSQL'], ARRAY['REST', 'SQL', 'JPA'], 4,
     NULL::text, 'student@kernel.ac.in', 'lead.backend@kernel.ac.in', NULL::text),
    ('ML Study Buddy', 'AI/ML/IoT', 'ACTIVE',
     'Members struggle to find structured ML study paths',
     'Spaced-repetition study plans generated from curated topics',
     ARRAY['Python', 'FastAPI', 'scikit-learn'], ARRAY['Python', 'Machine Learning'], 3,
     NULL::text, 'student@kernel.ac.in', 'lead.backend@kernel.ac.in', NULL::text),
    ('CP Judge Lite', 'Competitive Programming', 'PROPOSED',
     'Club contests rely on external judges with no club-specific tests',
     'Self-hosted judge for weekly club contests',
     ARRAY['Go', 'Docker'], ARRAY['Algorithms', 'Systems'], 3,
     NULL::text, 'student@kernel.ac.in', NULL::text, NULL::text),
    ('Portfolio Reviewer', 'Frontend', 'CHANGES_REQUESTED',
     'Members get inconsistent portfolio feedback',
     'Peer review rounds with rubrics for portfolio sites',
     ARRAY['React', 'TypeScript'], ARRAY['React', 'Accessibility'], 4,
     NULL::text, 'student@kernel.ac.in', NULL::text,
     'Narrow the scope to review scheduling only; clarify the moderation flow.'),
    ('Smart Attendance Bot', 'Backend', 'COMPLETED',
     'Manual attendance in sessions is slow and error prone',
     'A chat bot that records attendance during live sessions',
     ARRAY['Python', 'Telegram API'], ARRAY['Python', 'APIs'], 3,
     'Shipped to 3 club sessions with full adoption.',
     'student@kernel.ac.in', 'lead.backend@kernel.ac.in', NULL::text),
    ('Legacy Site Refresh', 'General', 'ARCHIVED',
     'The old club website is stale and unmaintainable',
     'Static redesign with a content pipeline',
     ARRAY['HTML', 'CSS'], ARRAY['CSS'], 2,
     'Superseded by the main website redesign.',
     'student@kernel.ac.in', 'core@kernel.ac.in', NULL::text)
) AS v(name, domain_name, status, problem, solution, technologies,
       required_skills, expected_members, outcome, creator_email,
       approver_email, review_comment)
JOIN domains d ON d.name = v.domain_name
JOIN users c ON c.email = v.creator_email
LEFT JOIN users ap ON ap.email = v.approver_email;

-- ---------- PROJECT MEMBERS (9) ----------
INSERT INTO project_members (project_id, user_id, role, status, reviewed_by)
SELECT p.id, u.id, v.role, v.status, r.id
FROM (VALUES
    ('Campus Feed API', 'student@kernel.ac.in', 'OWNER', 'ACTIVE', 'lead.backend@kernel.ac.in'),
    ('Campus Feed API', 'lead.backend@kernel.ac.in', 'MEMBER', 'ACTIVE', 'lead.backend@kernel.ac.in'),
    ('Campus Feed API', 'lead.frontend@kernel.ac.in', 'MEMBER', 'REQUESTED', NULL::text),
    ('ML Study Buddy', 'student@kernel.ac.in', 'OWNER', 'ACTIVE', 'lead.backend@kernel.ac.in'),
    ('ML Study Buddy', 'lead.backend@kernel.ac.in', 'MEMBER', 'ACTIVE', 'lead.backend@kernel.ac.in'),
    ('Smart Attendance Bot', 'student@kernel.ac.in', 'OWNER', 'ACTIVE', 'lead.backend@kernel.ac.in'),
    ('Smart Attendance Bot', 'core@kernel.ac.in', 'MEMBER', 'ACTIVE', 'lead.backend@kernel.ac.in'),
    ('Smart Attendance Bot', 'lead.backend@kernel.ac.in', 'MEMBER', 'ACTIVE', 'lead.backend@kernel.ac.in'),
    ('Legacy Site Refresh', 'student@kernel.ac.in', 'OWNER', 'ACTIVE', 'core@kernel.ac.in')
) AS v(project_name, email, role, status, reviewer_email)
JOIN projects p ON p.name = v.project_name
JOIN users u ON u.email = v.email
LEFT JOIN users r ON r.email = v.reviewer_email;

-- ---------- WORK ITEMS (13; progress: Campus Feed 3/5 = 60%, Attendance Bot 4/4 = 100%) ----------
INSERT INTO work_items (project_id, title, assignee_id, status, priority, deadline)
SELECT p.id, v.title, a.id, v.status, v.priority, v.deadline
FROM (VALUES
    ('Campus Feed API', 'Design API contract', 'student@kernel.ac.in', 'DONE', 'HIGH',
     TIMESTAMP '2026-09-28 23:59'),
    ('Campus Feed API', 'Implement auth module', 'lead.backend@kernel.ac.in', 'DONE', 'HIGH',
     TIMESTAMP '2026-10-02 23:59'),
    ('Campus Feed API', 'Set up database schema', 'student@kernel.ac.in', 'DONE', 'MEDIUM',
     TIMESTAMP '2026-10-04 23:59'),
    ('Campus Feed API', 'Build feed endpoints', 'student@kernel.ac.in', 'IN_PROGRESS', 'HIGH',
     TIMESTAMP '2026-10-12 23:59'),
    ('Campus Feed API', 'Write integration tests', 'lead.backend@kernel.ac.in', 'TODO', 'MEDIUM',
     TIMESTAMP '2026-10-16 23:59'),
    ('ML Study Buddy', 'Collect dataset', 'student@kernel.ac.in', 'DONE', 'MEDIUM',
     TIMESTAMP '2026-10-01 23:59'),
    ('ML Study Buddy', 'Baseline model', 'lead.backend@kernel.ac.in', 'IN_PROGRESS', 'HIGH',
     TIMESTAMP '2026-10-14 23:59'),
    ('ML Study Buddy', 'API wrapper', 'student@kernel.ac.in', 'TODO', 'MEDIUM',
     TIMESTAMP '2026-10-20 23:59'),
    ('ML Study Buddy', 'Evaluation dashboard', NULL::text, 'TODO', 'LOW',
     TIMESTAMP '2026-10-25 23:59'),
    ('Smart Attendance Bot', 'Bot skeleton', 'student@kernel.ac.in', 'DONE', 'HIGH',
     TIMESTAMP '2026-09-20 23:59'),
    ('Smart Attendance Bot', 'Session reminders', 'core@kernel.ac.in', 'DONE', 'MEDIUM',
     TIMESTAMP '2026-09-24 23:59'),
    ('Smart Attendance Bot', 'Attendance commands', 'lead.backend@kernel.ac.in', 'DONE', 'HIGH',
     TIMESTAMP '2026-09-27 23:59'),
    ('Smart Attendance Bot', 'Deploy script', 'student@kernel.ac.in', 'DONE', 'LOW',
     TIMESTAMP '2026-09-30 23:59')
) AS v(project_name, title, assignee_email, status, priority, deadline)
JOIN projects p ON p.name = v.project_name
LEFT JOIN users a ON a.email = v.assignee_email;

-- ---------- ACTIVITY TIMELINE (10, append-only) ----------
INSERT INTO activities (project_id, actor_id, kind, text, evidence_ref, created_at)
SELECT p.id, u.id, v.kind, v.text, v.evidence_ref, v.created_at
FROM (VALUES
    ('Campus Feed API', 'student@kernel.ac.in', 'PROPOSAL_SUBMITTED',
     'Alex Thomas proposed Campus Feed API', NULL::text, now() - interval '16 days'),
    ('Campus Feed API', 'lead.backend@kernel.ac.in', 'REVIEW_DECISION',
     'Prem Kumar approved the proposal', NULL::text, now() - interval '15 days'),
    ('Campus Feed API', 'lead.backend@kernel.ac.in', 'MEMBER_JOINED',
     'Prem Kumar joined as member', NULL::text, now() - interval '15 days'),
    ('Campus Feed API', 'student@kernel.ac.in', 'WORK_ITEM_CREATED',
     'Added work item: Design API contract', NULL::text, now() - interval '14 days'),
    ('Campus Feed API', 'student@kernel.ac.in', 'WORK_ITEM_DONE',
     'Completed work item: Design API contract', NULL::text, now() - interval '9 days'),
    ('Campus Feed API', 'student@kernel.ac.in', 'TASK_COMPLETED',
     'Submitted Build a CRUD API with commit a1b2c3d4',
     'https://github.com/alex-thomas/campus-api', now() - interval '8 days'),
    ('Portfolio Reviewer', 'student@kernel.ac.in', 'PROPOSAL_SUBMITTED',
     'Alex Thomas proposed Portfolio Reviewer', NULL::text, now() - interval '6 days'),
    ('Portfolio Reviewer', 'lead.backend@kernel.ac.in', 'REVIEW_DECISION',
     'Prem Kumar requested changes', NULL::text, now() - interval '5 days'),
    ('CP Judge Lite', 'student@kernel.ac.in', 'PROPOSAL_SUBMITTED',
     'Alex Thomas proposed CP Judge Lite', NULL::text, now() - interval '2 days'),
    ('Smart Attendance Bot', 'lead.backend@kernel.ac.in', 'PROJECT_STATUS_CHANGED',
     'Project marked COMPLETED', NULL::text, now() - interval '4 days')
) AS v(project_name, email, kind, text, evidence_ref, created_at)
JOIN projects p ON p.name = v.project_name
JOIN users u ON u.email = v.email;

-- ---------- DISCUSSION MESSAGES (6, project scope, poll 10s) ----------
INSERT INTO messages (project_id, sender_id, content, created_at)
SELECT p.id, u.id, v.content, v.created_at
FROM (VALUES
    ('Campus Feed API', 'student@kernel.ac.in', 'Kicked off the API contract draft, please review.',
     now() - interval '14 days'),
    ('Campus Feed API', 'lead.backend@kernel.ac.in', 'Looks good - use cursor pagination for the feed.',
     now() - interval '13 days'),
    ('Campus Feed API', 'lead.frontend@kernel.ac.in', 'Joining today, will take the feed endpoints.',
     now() - interval '10 days'),
    ('Campus Feed API', 'student@kernel.ac.in', 'Auth module merged.',
     now() - interval '7 days'),
    ('Campus Feed API', 'lead.backend@kernel.ac.in', 'Nice, add integration tests next.',
     now() - interval '6 days'),
    ('Campus Feed API', 'lead.frontend@kernel.ac.in', 'Working on feed endpoints now.',
     now() - interval '1 days')
) AS v(project_name, email, content, created_at)
JOIN projects p ON p.name = v.project_name
JOIN users u ON u.email = v.email;

-- ---------- XP LEDGER (15; amounts per blueprint XP table) ----------
INSERT INTO xp_events (user_id, amount, source, ref_type, ref_id)
SELECT u.id, v.amount, v.source, v.ref_type, v.ref_id
FROM (VALUES
    ('student@kernel.ac.in', 5, 'SESSION', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 5, 'SESSION', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 5, 'SESSION', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 10, 'RESOURCE', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 15, 'TASK', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 15, 'TASK', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 30, 'PR_MERGED', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 40, 'CONTRIBUTION', NULL::text, NULL::bigint),
    ('student@kernel.ac.in', 100, 'PROJECT_DONE', NULL::text, NULL::bigint),
    ('core@kernel.ac.in', 5, 'SESSION', NULL::text, NULL::bigint),
    ('core@kernel.ac.in', 40, 'CONTRIBUTION', NULL::text, NULL::bigint),
    ('lead.backend@kernel.ac.in', 5, 'SESSION', NULL::text, NULL::bigint),
    ('lead.backend@kernel.ac.in', 20, 'MENTORING', NULL::text, NULL::bigint),
    ('lead.backend@kernel.ac.in', 20, 'MENTORING', NULL::text, NULL::bigint),
    ('lead.frontend@kernel.ac.in', 5, 'SESSION', NULL::text, NULL::bigint)
) AS v(email, amount, source, ref_type, ref_id)
JOIN users u ON u.email = v.email;

-- ---------- SKILL PROFILE (10, derived 0-100) ----------
INSERT INTO skills (user_id, domain_id, score)
SELECT u.id, d.id, v.score
FROM (VALUES
    ('student@kernel.ac.in', 'Backend', 72),
    ('student@kernel.ac.in', 'Frontend', 45),
    ('student@kernel.ac.in', 'AI/ML/IoT', 30),
    ('student@kernel.ac.in', 'Competitive Programming', 55),
    ('student@kernel.ac.in', 'General', 20),
    ('core@kernel.ac.in', 'General', 65),
    ('core@kernel.ac.in', 'Backend', 40),
    ('lead.backend@kernel.ac.in', 'Backend', 88),
    ('lead.backend@kernel.ac.in', 'AI/ML/IoT', 80),
    ('lead.frontend@kernel.ac.in', 'Frontend', 75)
) AS v(email, domain_name, score)
JOIN users u ON u.email = v.email
JOIN domains d ON d.name = v.domain_name;

-- ---------- EARNED ACHIEVEMENTS (2) ----------
INSERT INTO user_achievements (user_id, achievement_id)
SELECT u.id, a.id
FROM (VALUES
    ('student@kernel.ac.in', 'FIRST_TASK'),
    ('lead.backend@kernel.ac.in', 'SPECIALIST')
) AS v(email, achievement_code)
JOIN users u ON u.email = v.email
JOIN achievements a ON a.code = v.achievement_code;

-- ---------- NOTIFICATIONS (4) ----------
INSERT INTO notifications (user_id, kind, title, body, link, read)
SELECT u.id, v.kind, v.title, v.body, v.link, v.read
FROM (VALUES
    ('student@kernel.ac.in', 'TASK_ASSIGNED', 'Task assigned: Add JWT security',
     'Prem assigned you a task in REST APIs with Spring Boot', '/sessions', FALSE),
    ('student@kernel.ac.in', 'ACHIEVEMENT_UNLOCKED', 'Achievement unlocked: First Task',
     'You completed your first verified task', '/dashboard', TRUE),
    ('lead.backend@kernel.ac.in', 'JOIN_REQUESTED', 'Join request: Campus Feed API',
     'Sara Khan requested to join Campus Feed API', '/projects', FALSE),
    ('core@kernel.ac.in', 'PROJECT_STATUS_CHANGED', 'Smart Attendance Bot completed',
     'The project you follow is now COMPLETED', '/projects', FALSE)
) AS v(email, kind, title, body, link, read)
JOIN users u ON u.email = v.email;

-- ---------- AUDIT LOG (3, append-only) ----------
INSERT INTO audit_logs (actor_id, action, resource, resource_id, meta)
SELECT u.id, 'ROLE_MANAGE', 'USER', t.id, '{"role": "DOMAIN_LEAD"}'::jsonb
FROM users u
JOIN users t ON t.email = 'lead.backend@kernel.ac.in'
WHERE u.email = 'superadmin@kernel.ac.in';

INSERT INTO audit_logs (actor_id, action, resource, resource_id, meta)
SELECT u.id, 'PROJECT_APPROVE', 'PROJECT', p.id, '{"decision": "APPROVE"}'::jsonb
FROM users u
JOIN projects p ON p.name = 'Campus Feed API'
WHERE u.email = 'lead.backend@kernel.ac.in';

INSERT INTO audit_logs (actor_id, action, resource, resource_id, meta)
SELECT u.id, 'TASK_VERIFY', 'TASK', t.id, NULL::jsonb
FROM users u
JOIN tasks t ON t.title = 'Build a CRUD API'
WHERE u.email = 'lead.backend@kernel.ac.in';
