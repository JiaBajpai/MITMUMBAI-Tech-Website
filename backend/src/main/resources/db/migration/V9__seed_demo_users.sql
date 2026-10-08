-- V9: demo users - 6 accounts covering every RBAC role (blueprint day 1 task 1.2)
-- Demo password for ALL seeded accounts: Kernel@123  (BCrypt-12, $2b$)

INSERT INTO users (email, name, password_hash, program) VALUES
    ('superadmin@kernel.ac.in', 'Aarav Sharma',
     '$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6', 'TECHNICAL'),
    ('core@kernel.ac.in', 'Diya Patel',
     '$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6', 'TECHNICAL'),
    ('lead.backend@kernel.ac.in', 'Prem Kumar',
     '$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6', 'TECHNICAL'),
    ('lead.frontend@kernel.ac.in', 'Sara Khan',
     '$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6', 'TECHNICAL'),
    ('faculty@kernel.ac.in', 'Prof. R. Iyer',
     '$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6', 'TECHNICAL'),
    ('student@kernel.ac.in', 'Alex Thomas',
     '$2b$12$0ZSBSEuc6v6CIZVeer94iukz/fusV5cPQtMjBRPLyq9V7f5ma1mJ6', 'TECHNICAL');

INSERT INTO user_roles (user_id, role)
SELECT u.id, r.role
FROM users u
JOIN (VALUES
    ('superadmin@kernel.ac.in', 'SUPER_ADMIN'),
    ('core@kernel.ac.in', 'CORE_MEMBER'),
    ('lead.backend@kernel.ac.in', 'DOMAIN_LEAD'),
    ('lead.frontend@kernel.ac.in', 'DOMAIN_LEAD'),
    ('faculty@kernel.ac.in', 'FACULTY'),
    ('student@kernel.ac.in', 'STUDENT')
) AS r(email, role) ON r.email = u.email;

INSERT INTO user_profiles (user_id, bio, github_url, linkedin_url)
SELECT id, 'Kernel Club member', NULL, NULL FROM users
WHERE email IN ('lead.backend@kernel.ac.in', 'student@kernel.ac.in');

UPDATE user_profiles SET github_url = 'https://github.com/prem-kumar'
WHERE user_id = (SELECT id FROM users WHERE email = 'lead.backend@kernel.ac.in');

UPDATE user_profiles SET github_url = 'https://github.com/alex-thomas',
    bio = 'Backend + CP, loves shipping side projects'
WHERE user_id = (SELECT id FROM users WHERE email = 'student@kernel.ac.in');

-- Prem leads Backend + AI/ML/IoT; Sara leads Frontend (blueprint 1.3 example)
INSERT INTO domain_leads (domain_id, user_id, assigned_by)
SELECT d.id, u.id, (SELECT id FROM users WHERE email = 'superadmin@kernel.ac.in')
FROM domains d
JOIN users u ON u.email = 'lead.backend@kernel.ac.in'
WHERE d.name IN ('Backend', 'AI/ML/IoT');

INSERT INTO domain_leads (domain_id, user_id, assigned_by)
SELECT d.id, u.id, (SELECT id FROM users WHERE email = 'superadmin@kernel.ac.in')
FROM domains d
JOIN users u ON u.email = 'lead.frontend@kernel.ac.in'
WHERE d.name = 'Frontend';

INSERT INTO domain_members (domain_id, user_id)
SELECT d.id, u.id
FROM users u
JOIN (VALUES
    ('lead.backend@kernel.ac.in', 'Backend'),
    ('lead.backend@kernel.ac.in', 'AI/ML/IoT'),
    ('lead.frontend@kernel.ac.in', 'Frontend'),
    ('core@kernel.ac.in', 'General'),
    ('superadmin@kernel.ac.in', 'General'),
    ('student@kernel.ac.in', 'Frontend'),
    ('student@kernel.ac.in', 'Backend')
) AS m(email, domain_name) ON m.email = u.email
JOIN domains d ON d.name = m.domain_name
ON CONFLICT DO NOTHING;
