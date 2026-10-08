-- V8: reference data seeds - 5 fixed domains + 5 MVP achievements (blueprint section 9)

INSERT INTO domains (name, description, display_order) VALUES
    ('General', 'Club-wide announcements, mixed topics and open discussions', 1),
    ('Frontend', 'Web UI, design systems, React and browser platforms', 2),
    ('Backend', 'APIs, databases, systems design and cloud services', 3),
    ('AI/ML/IoT', 'Machine learning, data and embedded/IoT projects', 4),
    ('Competitive Programming', 'Algorithms, contests and problem solving', 5);

INSERT INTO achievements (code, name, description, rule) VALUES
    ('FIRST_TASK', 'First Task', 'Complete your first task',
     'task_completions_count >= 1'),
    ('BUILDER', 'Builder', 'Complete your first project work item',
     'work_items_done_count >= 1'),
    ('TEN_TASKS', 'Task Master', 'Complete ten tasks',
     'task_completions_count >= 10'),
    ('MENTOR', 'Mentor', 'Earn XP through mentoring other members',
     'mentoring_xp > 0'),
    ('SPECIALIST', 'Specialist', 'Reach a skill score above 80 in any domain',
     'max_skill_score > 80');
